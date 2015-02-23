/*
 * Copyright (C) 2010-2011 The University of Manchester
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

import java.io.File;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.factories.ConfigurableRunFactory;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * A simple factory for workflow runs that forks runs from a subprocess.
 * 
 * @author Donal Fellows
 */
@ManagedResource(objectName = JMX_ROOT + "RunFactory", description = "The factory for simple singleton forked run.")
public class ForkRunFactory extends AbstractRemoteRunFactory implements
		ConfigurableRunFactory {
	private int lastStartupCheckCount;
	private Integer lastExitCode;
	private RemoteRunFactory factory;
	private Process factoryProcess;
	private String factoryProcessName;

	/**
	 * Create a factory for remote runs that works by forking off a subprocess.
	 * 
	 * @throws JAXBException
	 *             Shouldn't happen.
	 */
	public ForkRunFactory() throws JAXBException {
	}

	@PostConstruct
	protected void initRegistry() {
		log.info("waiting for availability of default RMI registry");
		getTheRegistry();
	}

	@Override
	protected void reinitFactory() {
		boolean makeFactory = factory != null;
		killFactory();
		try {
			if (makeFactory)
				initFactory();
		} catch (Exception e) {
			log.fatal("failed to make connection to remote run factory", e);
		}
	}

	private RemoteRunFactory getFactory() throws RemoteException {
		try {
			initFactory();
		} catch (RemoteException e) {
			throw e;
		} catch (Exception e) {
			throw new RemoteException("problem constructing factory", e);
		}
		return factory;
	}

	/**
	 * @return How many checks were done for the worker process the last time a
	 *         spawn was tried.
	 */
	@ManagedAttribute(description = "How many checks were done for the worker process the last time a spawn was tried.", currencyTimeLimit = 60)
	@Override
	public int getLastStartupCheckCount() {
		return lastStartupCheckCount;
	}

	/**
	 * @return What was the exit code from the last time the factory subprocess
	 *         was killed?
	 */
	@ManagedAttribute(description = "What was the exit code from the last time the factory subprocess was killed?")
	@Override
	public Integer getLastExitCode() {
		return lastExitCode;
	}

	/**
	 * @return What the factory subprocess's main RMI interface is registered
	 *         as.
	 */
	@ManagedAttribute(description = "What the factory subprocess's main RMI interface is registered as.", currencyTimeLimit = 60)
	@Override
	public String getFactoryProcessName() {
		return factoryProcessName;
	}

	/**
	 * Makes the subprocess that manufactures runs.
	 * 
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public void initFactory() throws Exception {
		if (factory != null)
			return;
		// Generate the arguments to use when spawning the subprocess
		factoryProcessName = state.getFactoryProcessNamePrefix() + randomUUID();
		ProcessBuilder p = new ProcessBuilder(getJavaBinary());
		p.command().add("-jar");
		p.command().add(getServerWorkerJar());
		if (getExecuteWorkflowScript() == null)
			log.fatal("no execute workflow script");
		p.command().add(getExecuteWorkflowScript());
		p.command().addAll(asList(getExtraArguments()));
		p.command().add(factoryProcessName);
		p.redirectErrorStream(true);
		p.directory(new File(getProperty("javax.servlet.context.tempdir",
				getProperty("java.io.tmpdir"))));

		// Spawn the subprocess
		log.info("about to create subprocess: " + p.command());
		
		factoryProcess = launchSubprocess(p);
		outlog = new StreamLogger("FactoryStdout", factoryProcess.getInputStream()) {
			@Override
			protected void write(String msg) {
				log.info(msg);
			}
		};
		errlog = new StreamLogger("FactoryStderr", factoryProcess.getErrorStream()) {
			@Override
			protected void write(String msg) {
				log.info(msg);
			}
		};

		// Wait for the subprocess to register itself in the RMI registry
		Calendar deadline = Calendar.getInstance();
		deadline.add(SECOND, state.getWaitSeconds());
		Exception lastException = null;
		lastStartupCheckCount = 0;
		while (deadline.after(Calendar.getInstance())) {
			try {
				sleep(state.getSleepMS());
				lastStartupCheckCount++;
				factory = getRemoteFactoryHandle(factoryProcessName);
				initInteractionDetails(factory);
				return;
			} catch (InterruptedException ie) {
				continue;
			} catch (NotBoundException nbe) {
				lastException = nbe;
				log.info("resource \"" + factoryProcessName
						+ "\" not yet registered...");
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
			} catch (RuntimeException e) {
				lastException = e;
			}
		}
		if (lastException == null)
			lastException = new InterruptedException();
		throw lastException;
	}

	private StreamLogger outlog, errlog;

	private void stopLoggers() {
		if (outlog != null)
			outlog.stop();
		outlog = null;
		if (errlog != null)
			errlog.stop();
		errlog = null;
	}

	private RemoteRunFactory getRemoteFactoryHandle(String name)
			throws RemoteException, NotBoundException {
		log.info("about to look up resource called " + name);
		try {
			// Validate registry connection first
			getTheRegistry().list();
		} catch (ConnectException | ConnectIOException e) {
			log.warn("connection problems with registry", e);
		}
		RemoteRunFactory rrf = (RemoteRunFactory) getTheRegistry().lookup(name);
		log.info("successfully connected to factory subprocess "
				+ factoryProcessName);
		return rrf;
	}

	/**
	 * Destroys the subprocess that manufactures runs.
	 */
	@PreDestroy
	public void killFactory() {
		if (factory != null) {
			log.info("requesting shutdown of " + factoryProcessName);
			try {
				factory.shutdown();
				sleep(700);
			} catch (RemoteException e) {
				log.warn(factoryProcessName + " failed to shut down nicely", e);
			} catch (InterruptedException e) {
				if (log.isDebugEnabled())
					log.debug("interrupted during wait after asking "
							+ factoryProcessName + " to shut down", e);
			} finally {
				factory = null;
			}
		}

		if (factoryProcess != null) {
			int code = -1;
			try {
				lastExitCode = code = factoryProcess.exitValue();
				log.info(factoryProcessName + " already dead?");
			} catch (RuntimeException e) {
				log.info("trying to force death of " + factoryProcessName);
				try {
					factoryProcess.destroy();
					sleep(350); // takes a little time, even normally
					lastExitCode = code = factoryProcess.exitValue();
				} catch (Exception e2) {
					code = -1;
				}
			} finally {
				factoryProcess = null;
				stopLoggers();
			}
			if (code > 128) {
				log.info(factoryProcessName + " died with signal="
						+ (code - 128));
			} else if (code >= 0) {
				log.info(factoryProcessName + " process killed: code=" + code);
			} else {
				log.warn(factoryProcessName + " not yet dead");
			}
		}
	}

	/**
	 * The real core of the run builder, factored out from its reliability
	 * support.
	 * 
	 * @param creator
	 *            Who created this workflow?
	 * @param wf
	 *            The serialized workflow.
	 * @return The remote handle of the workflow run.
	 * @throws RemoteException
	 *             If anything fails (communications error, etc.)
	 */
	private RemoteSingleRun getRealRun(@Nonnull UsernamePrincipal creator,
			@Nonnull byte[] wf, UUID id) throws RemoteException {
		@Nonnull
		String globaluser = "Unknown Person";
		if (creator != null)
			globaluser = creator.getName();
		RemoteSingleRun rsr = getFactory().make(wf, globaluser,
				makeURReciver(creator), id);
		incrementRunCount();
		return rsr;
	}

	@Override
	protected RemoteSingleRun getRealRun(UsernamePrincipal creator,
			Workflow workflow, UUID id) throws Exception {
		@Nonnull
		byte[] wf = serializeWorkflow(workflow);
		for (int i = 0; i < 3; i++) {
			initFactory();
			try {
				return getRealRun(creator, wf, id);
			} catch (ConnectException | ConnectIOException e) {
				// factory was lost; try to recreate
			}
			killFactory();
		}
		throw new NoCreateException("total failure to connect to factory "
				+ factoryProcessName + "despite attempting restart");
	}

	@Override
	public String[] getFactoryProcessMapping() {
		return new String[0];
	}

	@Override
	protected int operatingCount() throws Exception {
		return getFactory().countOperatingRuns();
	}
}
