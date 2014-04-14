/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.System.getSecurityManager;
import static java.lang.System.setProperty;
import static java.lang.System.setSecurityManager;
import static java.rmi.registry.LocateRegistry.createRegistry;
import static java.rmi.registry.LocateRegistry.getRegistry;
import static java.rmi.registry.Registry.REGISTRY_PORT;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.TavernaServer.JMX_ROOT;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.DIR;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.server.UsageRecordReceiver;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interaction.InteractionFeedSupport;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.SecurityContextFactory;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.UriBuilderFactory;
import org.taverna.server.master.notification.atom.EventDAO;
import org.taverna.server.master.usage.UsageRecordRecorder;
import org.taverna.server.master.utils.UsernamePrincipal;
import org.taverna.server.master.worker.FactoryBean;
import org.taverna.server.master.worker.RemoteRunDelegate;
import org.taverna.server.master.worker.RunFactoryConfiguration;

/**
 * Bridge to remote runs via RMI.
 * 
 * @author Donal Fellows
 */
@ManagedResource(objectName = JMX_ROOT + "Factory", description = "The factory for runs")
public abstract class AbstractRemoteRunFactory extends RunFactoryConfiguration
		implements ListenerFactory, RunFactory, FactoryBean {
	/**
	 * Whether to apply stronger limitations than normal to RMI. It is
	 * recommended that this be true!
	 */
	@Value("${rmi.localhostOnly}")
	private boolean rmiLocalhostOnly;
	/** The interaction host name. */
	private String interhost;
	/** The interaction port number. */
	private String interport;
	private Process registryProcess;
	/**
	 * The interaction WebDAV location. Will be resolved before being passed to
	 * the back-end.
	 */
	private String interwebdav;
	/**
	 * The interaction ATOM feed location. Will be resolved before being passed
	 * to the back-end.
	 */
	private String interfeed;
	/** Used for doing URI resolution. */
	@Resource(name = "webapp")
	private UriBuilderFactory baseurifactory;
	@Autowired
	private InteractionFeedSupport interactionFeedSupport;

	@Value("${taverna.interaction.host}")
	void setInteractionHost(String host) {
		if (host != null && host.equals("none"))
			host = null;
		interhost = host;
	}

	@Value("${taverna.interaction.port}")
	void setInteractionPort(String port) {
		if (port != null && port.equals("none"))
			port = null;
		interport = port;
	}

	@Value("${taverna.interaction.webdav_path}")
	void setInteractionWebdav(String webdav) {
		if (webdav != null && webdav.equals("none"))
			webdav = null;
		interwebdav = webdav;
	}

	@Value("${taverna.interaction.feed_path}")
	void setInteractionFeed(String feed) {
		if (feed != null && feed.equals("none"))
			feed = null;
		interfeed = feed;
	}

	@Override
	protected void reinitRegistry() {
		registry = null;
		if (registryProcess != null) {
			registryProcess.destroy();
			registryProcess = null;
		}
	}

	protected void initInteractionDetails(RemoteRunFactory factory)
			throws RemoteException {
		if (interhost != null) {
			String feed = baseurifactory.resolve(interfeed);
			String webdav = baseurifactory.resolve(interwebdav);
			factory.setInteractionServiceDetails(interhost, interport, webdav,
					feed);
		}
	}

	private Registry makeRegistry(int port) throws RemoteException {
		ProcessBuilder p = new ProcessBuilder(getJavaBinary());
		p.command().add("-jar");
		p.command().add(getRmiRegistryJar());
		p.command().add(Integer.toString(port));
		p.command().add(Boolean.toString(rmiLocalhostOnly));
		try {
			Process proc = p.start();
			Thread.sleep(getSleepTime());
			try {
				if (proc.exitValue() == 0)
					return null;
				String error = IOUtils.toString(proc.getErrorStream());
				throw new RemoteException(error);
			} catch (IllegalThreadStateException ise) {
				// Still running!
			}
			try (ObjectInputStream ois = new ObjectInputStream(
					proc.getInputStream())) {
				@SuppressWarnings("unchecked")
				Registry r = ((MarshalledObject<Registry>) ois.readObject())
						.get();
				registryProcess = proc;
				return r;
			}
		} catch (RemoteException e) {
			throw e;
		} catch (ClassNotFoundException e) {
			throw new RemoteException("unexpected registry type", e);
		} catch (IOException e) {
			throw new RemoteException("unexpected IO problem with registry", e);
		} catch (InterruptedException e) {
			throw new RemoteException("unexpected interrupt");
		}
	}

	/**
	 * @return A handle to the current RMI registry.
	 */
	protected Registry getTheRegistry() {
		try {
			if (registry != null) {
				registry.list();
				return registry;
			}
		} catch (RemoteException e) {
			log.warn("non-functioning existing registry handle", e);
			registry = null;
		}
		try {
			registry = getRegistry(getRegistryHost(), getRegistryPort());
			registry.list();
			return registry;
		} catch (RemoteException e) {
			log.warn("Failed to get working RMI registry handle.");
			registry = null;
			log.warn("Will build new registry, "
					+ "but service restart ability is at risk.");
			try {
				registry = makeRegistry(getRegistryPort());
				registry.list();
				return registry;
			} catch (RemoteException e2) {
				log.error(
						"failed to create local working RMI registry on port "
								+ getRegistryPort(), e2);
				log.info("original connection exception", e);
			}
		}
		try {
			registry = getRegistry(getRegistryHost(), REGISTRY_PORT);
			registry.list();
			return registry;
		} catch (RemoteException e) {
			log.warn("Failed to get working RMI registry handle on backup port.");
			try {
				registry = makeRegistry(REGISTRY_PORT);
				registry.list();
				return registry;
			} catch (RemoteException e2) {
				log.fatal(
						"totally failed to get registry handle, even on fallback!",
						e2);
				log.info("original connection exception", e);
				registry = null;
				throw new RuntimeException("No RMI Registry Available");
			}
		}
	}

	private Registry registry;
	/**
	 * The name of the resource that describes the default security policy to
	 * install.
	 */
	public static final String SECURITY_POLICY_FILE = "security.policy";
	private SecurityContextFactory securityFactory;
	UsageRecordRecorder usageRecordSink;
	TaskExecutor urProcessorPool;
	private EventDAO masterEventFeed;

	@Autowired(required = true)
	void setSecurityContextFactory(SecurityContextFactory factory) {
		this.securityFactory = factory;
	}

	@Autowired(required = true)
	void setMasterEventFeed(EventDAO masterEventFeed) {
		this.masterEventFeed = masterEventFeed;
	}

	@Autowired(required = true)
	void setUsageRecordSink(UsageRecordRecorder usageRecordSink) {
		this.usageRecordSink = usageRecordSink;
	}

	@Resource(name = "URThreads")
	@Required
	void setURProcessorPool(TaskExecutor urProcessorPool) {
		this.urProcessorPool = urProcessorPool;
	}

	/**
	 * Configures the Java security model. Not currently used, as it is
	 * viciously difficult to get right!
	 */
	@SuppressWarnings("unused")
	private static void installSecurityManager() {
		if (getSecurityManager() == null) {
			setProperty("java.security.policy", AbstractRemoteRunFactory.class
					.getClassLoader().getResource(SECURITY_POLICY_FILE)
					.toExternalForm());
			setSecurityManager(new RMISecurityManager());
		}
	}

	// static {
	// installSecurityManager();
	// }

	/**
	 * Set up the run expiry management engine.
	 * 
	 * @throws JAXBException
	 */
	public AbstractRemoteRunFactory() throws JAXBException {
		try {
			registry = LocateRegistry.getRegistry();
			registry.list();
		} catch (RemoteException e) {
			log.warn("Failed to get working RMI registry handle.");
			log.warn("Will build new registry, but service restart ability is at risk.");
			try {
				registry = createRegistry(REGISTRY_PORT);
				registry.list();
			} catch (RemoteException e2) {
				log.error("failed to create working RMI registry", e2);
				log.info("original connection exception", e);
			}
		}
	}

	@Override
	public List<String> getSupportedListenerTypes() {
		try {
			RemoteRunDelegate rrd = runDB.pickArbitraryRun();
			if (rrd != null)
				return rrd.getListenerTypes();
			log.warn("no remote runs; no listener types");
		} catch (Exception e) {
			log.warn("failed to get list of listener types", e);
		}
		return new ArrayList<>();
	}

	@Override
	public Listener makeListener(TavernaRun run, String listenerType,
			String configuration) throws NoListenerException {
		if (run instanceof RemoteRunDelegate)
			return ((RemoteRunDelegate) run).makeListener(listenerType,
					configuration);
		throw new NoListenerException("unexpected run type: " + run.getClass());
	}

	@Override
	public TavernaRun create(UsernamePrincipal creator, Workflow workflow)
			throws NoCreateException {
		try {
			Date now = new Date();
			UUID id = randomUUID();
			RemoteSingleRun rsr = getRealRun(creator, workflow, id);
			RemoteRunDelegate run = new RemoteRunDelegate(now, workflow, rsr,
					state.getDefaultLifetime(), runDB, id,
					state.getGenerateProvenance(), this);
			run.setSecurityContext(securityFactory.create(run, creator));
			URL feedUrl = interactionFeedSupport.getFeedURI(run).toURL();
			URL webdavUrl = baseurifactory.getRunUriBuilder(run)
					.path(DIR + "/interactions").build().toURL();
			rsr.setInteractionServiceDetails(feedUrl, webdavUrl);
			return run;
		} catch (NoCreateException e) {
			log.warn("failed to build run instance", e);
			throw e;
		} catch (Exception e) {
			log.warn("failed to build run instance", e);
			throw new NoCreateException("failed to build run instance", e);
		}
	}

	/**
	 * Gets the RMI connector for a new run.
	 * 
	 * @param creator
	 *            Who is creating the workflow run.
	 * @param workflow
	 *            What workflow are they instantiating.
	 * @param id
	 *            The identity token for the run, newly minted.
	 * @return The remote interface to the run.
	 * @throws Exception
	 *             Just about anything can go wrong...
	 */
	protected abstract RemoteSingleRun getRealRun(UsernamePrincipal creator,
			Workflow workflow, UUID id) throws Exception;

	/**
	 * How to convert a wrapped workflow into XML.
	 * 
	 * @param workflow
	 *            The wrapped workflow.
	 * @return The XML version of the document.
	 * @throws JAXBException
	 *             If serialization fails.
	 */
	protected String serializeWorkflow(Workflow workflow) throws JAXBException {
		return workflow.marshal();
	}

	/**
	 * Make a Remote object that can act as a consumer for usage records.
	 * 
	 * @param creator
	 * 
	 * @return The receiver, or <tt>null</tt> if the construction fails.
	 */
	protected UsageRecordReceiver makeURReciver(UsernamePrincipal creator) {
		try {
			@SuppressWarnings("serial")
			class URReceiver extends UnicastRemoteObject implements
					UsageRecordReceiver {
				public URReceiver() throws RemoteException {
					super();
				}

				@Override
				public void acceptUsageRecord(final String usageRecord) {
					if (usageRecordSink != null && urProcessorPool != null)
						urProcessorPool.execute(new Runnable() {
							@Override
							public void run() {
								usageRecordSink.storeUsageRecord(usageRecord);
							}
						});
					urProcessorPool.execute(new Runnable() {
						@Override
						public void run() {
							runDB.checkForFinishNow();
						}
					});
				}
			}
			return new URReceiver();
		} catch (RemoteException e) {
			log.warn("failed to build usage record receiver", e);
			return null;
		}
	}

	@Override
	public EventDAO getMasterEventFeed() {
		return masterEventFeed;
	}
}
