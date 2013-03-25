/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

//import static java.lang.System.getSecurityManager;
import static java.lang.System.setProperty;
//import static java.lang.System.setSecurityManager;
import static java.net.InetAddress.getLocalHost;
import static java.rmi.registry.LocateRegistry.createRegistry;
import static java.rmi.registry.LocateRegistry.getRegistry;
import static java.rmi.registry.Registry.REGISTRY_PORT;
import static java.rmi.server.RMISocketFactory.getDefaultSocketFactory;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.TavernaServerImpl.JMX_ROOT;

import java.io.IOException;
import java.net.ServerSocket;
//import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.server.UsageRecordReceiver;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.factories.ConfigurableRunFactory;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interaction.InteractionFeedSupport;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.SecurityContextFactory;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.usage.UsageRecordRecorder;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Bridge to remote runs via RMI.
 * 
 * @author Donal Fellows
 */
@ManagedResource(objectName = JMX_ROOT + "Factory", description = "The factory for runs")
public abstract class AbstractRemoteRunFactory implements ListenerFactory,
		RunFactory, ConfigurableRunFactory {
	/** The logger for writing messages out on. */
	Log log = LogFactory.getLog("Taverna.Server.LocalWorker");

	@SuppressWarnings("unused")
	@PreDestroy
	@edu.umd.cs.findbugs.annotations.SuppressWarnings("UPM_UNCALLED_PRIVATE_METHOD")
	private void closeLog() {
		log = null;
	}

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
		if (rmiLocalhostOnly) {
			setProperty("java.rmi.server.hostname", "127.0.0.1");
			return createRegistry(port, getDefaultSocketFactory(),
					new RMIServerSocketFactory() {
						@Override
						public ServerSocket createServerSocket(int port)
								throws IOException {
							return new ServerSocket(port, 0, getLocalHost());
						}
					});
		} else {
			return createRegistry(port);
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
	LocalWorkerState state;
	RunDBSupport runDB;
	private SecurityContextFactory securityFactory;
	UsageRecordRecorder usageRecordSink;
	TaskExecutor urProcessorPool;

	@Autowired(required = true)
	void setState(LocalWorkerState state) {
		this.state = state;
	}

	@Autowired(required = true)
	void setRunDB(RunDBSupport runDB) {
		this.runDB = runDB;
	}

	@Autowired(required = true)
	void setSecurityContextFactory(SecurityContextFactory factory) {
		this.securityFactory = factory;
	}

	@ManagedAttribute(description = "The host holding the RMI registry to communicate via.")
	@Override
	public String getRegistryHost() {
		return state.getRegistryHost();
	}

	@ManagedAttribute(description = "The host holding the RMI registry to communicate via.")
	@Override
	public void setRegistryHost(String host) {
		boolean rebuild = false;
		if (host == null || host.isEmpty()) {
			host = null;
			rebuild = (state.getRegistryHost() != null);
		} else {
			rebuild = !host.equals(state.getRegistryHost());
		}
		state.setRegistryHost(host);
		if (rebuild) {
			registry = null;
		}
	}

	@ManagedAttribute(description = "The port number of the RMI registry. Should not normally be set.")
	@Override
	public int getRegistryPort() {
		return state.getRegistryPort();
	}

	@ManagedAttribute(description = "The port number of the RMI registry. Should not normally be set.")
	@Override
	public void setRegistryPort(int port) {
		if (port != state.getRegistryPort())
			registry = null;
		state.setRegistryPort(port);
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

	// static {
	// if (getSecurityManager() == null) {
	// setProperty("java.security.policy", AbstractRemoteRunFactory.class
	// .getClassLoader().getResource(SECURITY_POLICY_FILE)
	// .toExternalForm());
	// setSecurityManager(new RMISecurityManager());
	// }
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
				return rrd.run.getListenerTypes();
			log.warn("no remote runs; no listener types");
		} catch (Exception e) {
			log.warn("failed to get list of listener types", e);
		}
		return new ArrayList<String>();
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
					state.getDefaultLifetime(), runDB, id, this);
			run.setSecurityContext(securityFactory.create(run, creator));
			rsr.setInteractionServiceDetails(
					interactionFeedSupport.getFeedURI(run).toURL(),
					baseurifactory.getRunUriBuilder(run)
							.path("wd/interactions").build().toURL());
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

	/** @return The names of the current runs. */
	@ManagedAttribute(description = "The names of the current runs.", currencyTimeLimit = 5)
	@Override
	public String[] getCurrentRunNames() {
		List<String> names = runDB.listRunNames();
		return names.toArray(new String[names.size()]);
	}

	@ManagedAttribute(description = "The maximum number of simultaneous runs supported by the server.", currencyTimeLimit = 300)
	@Override
	public int getMaxRuns() {
		return state.getMaxRuns();
	}

	@ManagedAttribute(description = "The maximum number of simultaneous runs supported by the server.")
	@Override
	public void setMaxRuns(int maxRuns) {
		state.setMaxRuns(maxRuns);
	}

	@ManagedAttribute(description = "The maximum number of simultaneous operating runs supported by the server.", currencyTimeLimit = 300)
	@Override
	public int getOperatingLimit() {
		return state.getOperatingLimit();
	}

	@ManagedAttribute(description = "The maximum number of simultaneous operating runs supported by the server.")
	@Override
	public void setOperatingLimit(int operatingLimit) {
		state.setOperatingLimit(operatingLimit);
	}

	/**
	 * @return A count of the number of runs believed to actually be in the
	 *         {@linkplain uk.org.taverna.server.master.common.Status#Operating
	 *         operating} state.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public abstract int getOperatingCount() throws Exception;

	/** @return How many minutes should a workflow live by default? */
	@ManagedAttribute(description = "How many minutes should a workflow live by default?", currencyTimeLimit = 300)
	@Override
	public int getDefaultLifetime() {
		return state.getDefaultLifetime();
	}

	/**
	 * Set how long a workflow should live by default.
	 * 
	 * @param defaultLifetime
	 *            Default lifetime, in minutes.
	 */
	@ManagedAttribute
	@Override
	public void setDefaultLifetime(int defaultLifetime) {
		state.setDefaultLifetime(defaultLifetime);
	}

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

	@Override
	public boolean isAllowingRunsToStart() {
		try {
			return state.getOperatingLimit() > getOperatingCount();
		} catch (Exception e) {
			log.info("failed to get operating run count", e);
			return false;
		}
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
			@edu.umd.cs.findbugs.annotations.SuppressWarnings({
					"SE_BAD_FIELD_INNER_CLASS", "SE_NO_SERIALVERSIONID" })
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
}
