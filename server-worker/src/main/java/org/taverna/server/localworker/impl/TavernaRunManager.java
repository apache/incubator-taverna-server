/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.lang.System.setProperty;
import static java.lang.System.setSecurityManager;
import static java.rmi.registry.LocateRegistry.getRegistry;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.server.UsageRecordReceiver;

import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The registered factory for runs, this class is responsible for constructing
 * runs that are suitable for particular users. It is also the entry point for
 * this whole process.
 * 
 * @author Donal Fellows
 * @see LocalWorker
 */
@SuppressWarnings({ "SE_BAD_FIELD", "SE_NO_SERIALVERSIONID" })
public class TavernaRunManager extends UnicastRemoteObject implements
		RemoteRunFactory {
	String command;
	RunFactory cons;
	Class<? extends Worker> workerClass;
	Map<String, String> seedEnvironment = new HashMap<String, String>();
	List<String> javaInitParams = new ArrayList<String>();
	private WorkflowBundleIO io;
	// Hacks!
	public static String interactionHost;
	public static String interactionPort;
	public static String interactionWebdavPath;
	public static String interactionFeedPath;

	private static final String usage = "java -jar server.worker.jar workflowExecScript ?-Ekey=val...? ?-Jconfig? UUID";

	/**
	 * An RMI-enabled factory for runs.
	 * 
	 * @param command
	 *            What command to call to actually run a run.
	 * @param constructor
	 *            What constructor to call to instantiate the RMI server object
	 *            for the run. The constructor <i>must</i> be able to take two
	 *            strings (the execution command, and the workflow document) and
	 *            a class (the <tt>workerClass</tt> parameter, below) as
	 *            arguments.
	 * @param workerClass
	 *            What class to create to actually manufacture and manage the
	 *            connection to the workflow engine.
	 * @throws RemoteException
	 *             If anything goes wrong during creation of the instance.
	 */
	public TavernaRunManager(String command, RunFactory constructor,
			Class<? extends Worker> workerClass) throws RemoteException {
		this.command = command;
		this.cons = constructor;
		this.workerClass = workerClass;
		this.io = new WorkflowBundleIO();
	}

	@Override
	public RemoteSingleRun make(byte[] workflow, String creator,
			UsageRecordReceiver urReceiver, UUID id) throws RemoteException {
		if (creator == null)
			throw new RemoteException("no creator");
		try {
			URI wfid = io.readBundle(new ByteArrayInputStream(workflow), null)
					.getMainWorkflow().getWorkflowIdentifier();
			out.println("Creating run from workflow <" + wfid + "> for <"
					+ creator + ">");
			return cons.construct(command, workflow, workerClass, urReceiver,
					id, seedEnvironment, javaInitParams);
		} catch (RemoteException e) {
			throw e;
		} catch (Exception e) {
			throw new RemoteException("bad instance construction", e);
		}
	}

	private static boolean shuttingDown;
	private static String factoryName;
	private static Registry registry;

	static synchronized void unregisterFactory() {
		if (!shuttingDown) {
			shuttingDown = true;
			try {
				if (factoryName != null && registry != null)
					registry.unbind(factoryName);
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		}
	}

	@Override
	public void shutdown() {
		unregisterFactory();
		new Thread(new DelayedDeath()).start();
	}

	static class DelayedDeath implements Runnable {
		@Override
		@SuppressWarnings("DM_EXIT")
		public void run() {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			} finally {
				exit(0);
			}
		}
	}

	/**
	 * The name of the file (in this code's resources) that provides the default
	 * security policy that we use.
	 */
	public static final String SECURITY_POLICY_FILE = "security.policy";

	private static final String SEC_POLICY_PROP = "java.security.policy";
	private static final String UNSECURE_PROP = "taverna.suppressrestrictions.rmi";
	private static final String RMI_HOST_PROP = "java.rmi.server.hostname";

	/**
	 * @param args
	 *            The arguments from the command line invocation.
	 * @throws Exception
	 *             If we can't connect to the RMI registry, or if we can't read
	 *             the workflow, or if we can't build the worker instance, or
	 *             register it. Also if the arguments are wrong.
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 2)
			throw new Exception("wrong # args: must be \"" + usage + "\"");
		if (!getProperty(UNSECURE_PROP, "no").equals("yes")) {
			setProperty(SEC_POLICY_PROP, LocalWorker.class.getClassLoader()
					.getResource(SECURITY_POLICY_FILE).toExternalForm());
			setProperty(RMI_HOST_PROP, "127.0.0.1");
		}
		setSecurityManager(new RMISecurityManager());
		String command = args[0];
		factoryName = args[args.length - 1];
		registry = getRegistry();
		TavernaRunManager man = new TavernaRunManager(command,
				new LocalWorker.Instantiate(), WorkerCore.class);
		for (int i = 1; i < args.length - 1; i++) {
			if (args[i].startsWith("-E")) {
				String arg = args[i].substring(2);
				int idx = arg.indexOf('=');
				if (idx > 0) {
					man.addEnvironmentDefinition(arg.substring(0, idx),
							arg.substring(idx + 1));
					continue;
				}
			} else if (args[i].startsWith("-D")) {
				if (args[i].indexOf('=') > 0) {
					man.addJavaParameter(args[i]);
					continue;
				}
			} else if (args[i].startsWith("-J")) {
				man.addJavaParameter(args[i].substring(2));
				continue;
			}
			throw new IllegalArgumentException("argument \"" + args[i]
					+ "\" must start with -D, -E or -J; "
					+ "-D and -E must contain a \"=\"");
		}
		registry.bind(factoryName, man);
		getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				unregisterFactory();
			}
		});
		out.println("registered RemoteRunFactory with ID " + factoryName);
	}

	private void addJavaParameter(String string) {
		this.javaInitParams.add(string);
	}

	private void addEnvironmentDefinition(String key, String value) {
		this.seedEnvironment.put(key, value);
	}

	@Override
	public void setInteractionServiceDetails(String host, String port,
			String webdavPath, String feedPath) throws RemoteException {
		if (host == null || port == null || webdavPath == null
				|| feedPath == null)
			throw new IllegalArgumentException("all params must be non-null");
		interactionHost = host;
		interactionPort = port;
		interactionWebdavPath = webdavPath;
		interactionFeedPath = feedPath;
	}

	/**
	 * How to actually make an instance of the local worker class.
	 * 
	 * @author Donal Fellows
	 */
	public interface RunFactory {
		/**
		 * Construct an instance of the workflow run access code.
		 * 
		 * @param executeWorkflowCommand
		 *            The script used to execute workflows.
		 * @param workflow
		 *            The workflow to execute.
		 * @param workerClass
		 *            The class to instantiate as our local representative of
		 *            the run.
		 * @param urReceiver
		 *            The remote class to report the generated usage record(s)
		 *            to.
		 * @param id
		 *            The UUID to use, or <tt>null</tt> if we are to invent one.
		 * @param seedEnvironment
		 *            The key/value pairs to seed the worker subprocess
		 *            environment with.
		 * @param javaParams
		 *            Parameters to pass to the worker subprocess java runtime
		 *            itself.
		 * @return The local worker class.
		 * @throws RemoteException
		 *             If registration of the worker fails.
		 * @throws ImplementationException
		 *             If something goes wrong during local setup.
		 */
		RemoteSingleRun construct(String executeWorkflowCommand,
				byte[] workflow, Class<? extends Worker> workerClass,
				UsageRecordReceiver urReceiver, UUID id,
				Map<String, String> seedEnvironment, List<String> javaParams)
				throws RemoteException, ImplementationException;
	}
}
