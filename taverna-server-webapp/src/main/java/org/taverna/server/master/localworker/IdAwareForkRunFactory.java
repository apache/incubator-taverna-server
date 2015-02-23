/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Calendar.SECOND;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.TavernaServer.JMX_ROOT;
import static org.taverna.server.master.localworker.AbstractRemoteRunFactory.launchSubprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.factories.ConfigurableRunFactory;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * A simple factory for workflow runs that forks runs from a subprocess.
 * 
 * @author Donal Fellows
 */
@ManagedResource(objectName = JMX_ROOT + "RunFactory", description = "The factory for a user-specific forked run.")
public class IdAwareForkRunFactory extends AbstractRemoteRunFactory implements
		ConfigurableRunFactory {
	private MetaFactory forker;
	private Map<String, RemoteRunFactory> factory;
	private Map<String, String> factoryProcessName;

	/**
	 * Create a factory for remote runs that works by forking off a subprocess.
	 * 
	 * @throws JAXBException
	 *             Shouldn't happen.
	 */
	public IdAwareForkRunFactory() throws JAXBException {
		factory = new HashMap<>();
		factoryProcessName = new HashMap<>();
	}

	@Override
	protected void reinitFactory() {
		boolean makeForker = forker != null;
		try {
			killForker();
		} catch (Exception e) {
			log.warn("exception when killing secure-fork process", e);
		}
		try {
			if (makeForker)
				initMetaFactory();
		} catch (Exception e) {
			log.fatal("failed to make secure-fork process", e);
		}
	}

	/**
	 * @return How many checks were done for the worker process the last time a
	 *         spawn was tried.
	 */
	@Override
	@ManagedAttribute(description = "How many checks were done for the worker process the last time a spawn was tried.", currencyTimeLimit = 60)
	public int getLastStartupCheckCount() {
		return forker == null ? 0 : forker.lastStartupCheckCount();
	}

	/**
	 * @return What was the exit code from the last time the factory subprocess
	 *         was killed?
	 */
	@Override
	@ManagedAttribute(description = "What was the exit code from the last time the factory subprocess was killed?")
	public Integer getLastExitCode() {
		return forker == null ? null : forker.lastExitCode();
	}

	/**
	 * @return The mapping of user names to RMI factory IDs.
	 */
	@Override
	@ManagedAttribute(description = "The mapping of user names to RMI factory IDs.", currencyTimeLimit = 60)
	public String[] getFactoryProcessMapping() {
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> keys = new ArrayList<>(factoryProcessName.keySet());
		String[] ks = keys.toArray(new String[keys.size()]);
		Arrays.sort(ks);
		for (String k : ks) {
			result.add(k);
			result.add(factoryProcessName.get(k));
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * How construction of factories is actually done.
	 * 
	 * @author Donal Fellows
	 */
	public interface MetaFactory {
		/**
		 * Make a factory for the given user.
		 * 
		 * @param username
		 *            Who to make it for.
		 * @return Handle of the factory.
		 * @throws Exception
		 *             If anything goes wrong.
		 */
		RemoteRunFactory make(String username) throws Exception;

		/**
		 * Shut down the meta-factory. It is not defined whether factories
		 * created by it are also shut down at the same time.
		 * 
		 * @throws IOException
		 *             If something goes wrong when communicating with the
		 *             meta-factory.
		 * @throws InterruptedException
		 *             If something stops us waiting for the shut down to
		 *             happen.
		 */
		void close() throws IOException, InterruptedException;

		int lastStartupCheckCount();

		Integer lastExitCode();
	}

	void registerFactory(String username, String fpn, RemoteRunFactory f) {
		factoryProcessName.put(username, fpn);
		factory.put(username, f);
	}

	/**
	 * Makes the connection to the meta-factory that makes factories.
	 * 
	 * @throws IOException
	 *             If the connection fails.
	 */
	@PostConstruct
	void initMetaFactory() throws IOException {
		log.info("waiting for availability of default RMI registry");
		getTheRegistry();
		log.info("constructing secure fork subprocess");
		forker = new SecureFork(this, state, log);
	}

	private void killForker() throws IOException, InterruptedException {
		try {
			if (forker != null)
				forker.close();
		} finally {
			forker = null;
		}
	}

	/**
	 * Makes the subprocess that manufactures runs.
	 * 
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	private void initFactory(String username) throws Exception {
		if (factory.containsKey(username))
			return;
		if (forker == null)
			initMetaFactory();
		forker.make(username);
	}

	/**
	 * Destroys the subprocess that manufactures runs.
	 */
	@PreDestroy
	public void killFactories() {
		if (!factory.isEmpty()) {
			Iterator<String> keys = factory.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				log.info("requesting shutdown of "
						+ factoryProcessName.get(key));
				try {
					factory.get(key).shutdown();
				} catch (RemoteException e) {
					log.warn(factoryProcessName.get(key)
							+ " failed to shut down nicely", e);
				} finally {
					keys.remove();
					factoryProcessName.remove(key);
				}
			}
			try {
				sleep(700);
			} catch (InterruptedException e) {
				if (log.isDebugEnabled())
					log.debug("interrupted during wait after "
							+ "asking factories to shut down", e);
			}
		}

		try {
			killForker();
		} catch (Exception e) {
			if (log.isDebugEnabled())
				log.debug("exception in shutdown of secure-fork process", e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		killFactories();
		super.finalize();
	}

	@Autowired
	public void setIdMapper(LocalIdentityMapper mapper) {
		this.mapper = mapper;
	}

	private LocalIdentityMapper mapper;

	/**
	 * The real core of the run builder, factored out from its reliability
	 * support.
	 * 
	 * @param creator
	 *            Who created this workflow?
	 * @param username
	 *            What user account is this workflow to be executed in?
	 * @param wf
	 *            The serialized workflow.
	 * @return The remote handle of the workflow run.
	 * @throws RemoteException
	 *             If anything fails (communications error, etc.)
	 */
	private RemoteSingleRun getRealRun(@Nonnull UsernamePrincipal creator,
			@Nonnull String username, @Nonnull byte[] wf, UUID id)
			throws RemoteException {
		String globaluser = "Unknown Person";
		if (creator != null)
			globaluser = creator.getName();
		RemoteSingleRun rsr = factory.get(username).make(wf, globaluser,
				makeURReciver(creator), id);
		incrementRunCount();
		return rsr;
	}

	@Override
	protected RemoteSingleRun getRealRun(UsernamePrincipal creator,
			Workflow workflow, UUID id) throws Exception {
		byte[] wf = serializeWorkflow(workflow);
		String username = mapper == null ? null : mapper
				.getUsernameForPrincipal(creator);
		if (username == null)
			throw new Exception("cannot determine who to run workflow as; "
					+ "local identity mapper returned null");
		for (int i = 0; i < 3; i++) {
			if (!factory.containsKey(username))
				initFactory(username);
			try {
				return getRealRun(creator, username, wf, id);
			} catch (ConnectException | ConnectIOException e) {
				// factory was lost; try to recreate
			}
			factory.remove(username);
		}
		throw new NoCreateException("total failure to connect to factory "
				+ factoryProcessName + "despite attempting restart");
	}

	@Value("${secureForkPasswordFile}")
	@Order(20)
	public void setPasswordSource(String passwordSource) {
		if (passwordSource == null || passwordSource.isEmpty()
				|| passwordSource.startsWith("${"))
			state.setDefaultPasswordFile(null);
		else
			state.setDefaultPasswordFile(passwordSource);
		if (state.getPasswordFile() == null)
			log.info("assuming password-free forking enabled");
		else
			log.info("configured secureForkPasswordFile from context as "
					+ state.getPasswordFile());
	}

	@Override
	public String getFactoryProcessName() {
		return "<PROPERTY-NOT-SUPPORTED>";
	}

	@Override
	protected int operatingCount() throws Exception {
		int total = 0;
		for (RemoteRunFactory rrf : factory.values())
			total += rrf.countOperatingRuns();
		return total;
	}
}

/**
 * The connector that handles the secure fork process itself.
 * 
 * @author Donal Fellows
 */
class SecureFork implements IdAwareForkRunFactory.MetaFactory {
	private IdAwareForkRunFactory main;
	private Process process;
	private PrintWriter channel;
	private int lastStartupCheckCount;
	private Integer lastExitCode;
	private Log log;
	private LocalWorkerState state;
	private StreamLogger out, err;

	/**
	 * Construct the command to run the meta-factory process.
	 * 
	 * @param args
	 *            The live list of arguments to pass.
	 */
	public void initFactoryArgs(List<String> args) {
		args.add(main.getJavaBinary());
		String pwf = main.getPasswordFile();
		if (pwf != null) {
			args.add("-Dpassword.file=" + pwf);
		}
		args.add("-jar");
		args.add(main.getServerForkerJar());
		args.add(main.getJavaBinary());
		args.add("-jar");
		args.add(main.getServerWorkerJar());
		if (main.getExecuteWorkflowScript() == null)
			log.fatal("no execute workflow script");
		args.add(main.getExecuteWorkflowScript());
		args.addAll(asList(main.getExtraArguments()));
	}

	SecureFork(IdAwareForkRunFactory main, LocalWorkerState state, Log log)
			throws IOException {
		this.main = main;
		this.log = log;
		this.state = state;
		ProcessBuilder p = new ProcessBuilder();
		initFactoryArgs(p.command());
		p.redirectErrorStream(true);
		p.directory(new File(getProperty("javax.servlet.context.tempdir",
				getProperty("java.io.tmpdir"))));

		// Spawn the subprocess
		log.info("about to create subprocess: " + p.command());
		log.info("subprocess directory: " + p.directory());
		process = launchSubprocess(p);
		channel = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				process.getOutputStream())), true);
		// Log the responses
		out = new StreamLogger("ForkedStdout", process.getInputStream()) {
			@Override
			protected void write(String msg) {
				log.info(msg);
			}
		};
		err = new StreamLogger("ForkedStderr", process.getErrorStream()) {
			@Override
			protected void write(String msg) {
				log.info(msg);
			}
		};
	}

	@Override
	public void close() throws IOException, InterruptedException {
		try {
			if (process != null) {
				log.info("about to close down subprocess");
				channel.close();
				int code = -1;
				try {
					try {
						code = process.exitValue();
						log.info("secure-fork process already dead?");
					} catch (IllegalThreadStateException e) {
						try {
							code = process.waitFor();
						} catch (InterruptedException e1) {
							log.info("interrupted waiting for natural death of secure-fork process?!");
							process.destroy();
							code = process.waitFor();
						}
					}
				} finally {
					lastExitCode = code;
					if (code > 128) {
						log.info("secure-fork process died with signal="
								+ (code - 128));
					} else if (code >= 0) {
						log.info("secure-fork process killed: code=" + code);
					} else {
						log.warn("secure-fork process not yet dead");
					}
				}
			}
		} finally {
			process = null;
			channel = null;
			out.stop();
			err.stop();
		}
	}

	protected void make(String username, String fpn) {
		log.info("about to request subprocess creation for " + username
				+ " producing ID " + fpn);
		channel.println(username + " " + fpn);
	}

	@Override
	public RemoteRunFactory make(String username) throws Exception {
		try {
			main.getTheRegistry().list(); // Validate registry connection first
		} catch (ConnectException | ConnectIOException e) {
			log.warn("connection problems with registry", e);
		} catch (RemoteException e) {
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			}
			log.warn("connection problems with registry", e);
		}

		String fpn = state.getFactoryProcessNamePrefix() + randomUUID();
		make(username, fpn);

		// Wait for the subprocess to register itself in the RMI registry
		Calendar deadline = Calendar.getInstance();
		deadline.add(SECOND, state.getWaitSeconds());
		Exception lastException = null;
		lastStartupCheckCount = 0;
		while (deadline.after(Calendar.getInstance())) {
			try {
				sleep(state.getSleepMS());
				lastStartupCheckCount++;
				log.info("about to look up resource called " + fpn);
				RemoteRunFactory f = (RemoteRunFactory) main.getTheRegistry()
						.lookup(fpn);
				log.info("successfully connected to factory subprocess " + fpn);
				main.initInteractionDetails(f);
				main.registerFactory(username, fpn, f);
				return f;
			} catch (InterruptedException ie) {
				continue;
			} catch (NotBoundException nbe) {
				lastException = nbe;
				log.info("resource \"" + fpn + "\" not yet registered...");
				continue;
			} catch (RemoteException re) {
				// Unpack a remote exception if we can
				lastException = re;
				try {
					if (re.getCause() != null)
						lastException = (Exception) re.getCause();
				} catch (Throwable t) {
					// Ignore!
				}
			} catch (Exception e) {
				lastException = e;
			}
		}
		if (lastException == null)
			lastException = new InterruptedException();
		throw lastException;
	}

	@Override
	public Integer lastExitCode() {
		return lastExitCode;
	}

	@Override
	public int lastStartupCheckCount() {
		return lastStartupCheckCount;
	}
}
