/*
 */
package org.taverna.server.localworker.impl;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.lang.Runtime.getRuntime;
import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.lang.System.setProperty;
import static java.lang.System.setSecurityManager;
import static java.rmi.registry.LocateRegistry.getRegistry;
import static org.taverna.server.localworker.api.Constants.DEATH_DELAY;
import static org.taverna.server.localworker.api.Constants.LOCALHOST;
import static org.taverna.server.localworker.api.Constants.RMI_HOST_PROP;
import static org.taverna.server.localworker.api.Constants.SECURITY_POLICY_FILE;
import static org.taverna.server.localworker.api.Constants.SEC_POLICY_PROP;
import static org.taverna.server.localworker.api.Constants.UNSECURE_PROP;

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

import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.taverna.server.localworker.api.RunAccounting;
import org.taverna.server.localworker.api.Worker;
import org.taverna.server.localworker.api.WorkerFactory;
import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.server.UsageRecordReceiver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * The registered factory for runs, this class is responsible for constructing
 * runs that are suitable for particular users. It is also the entry point for
 * this whole process.
 * 
 * @author Donal Fellows
 * @see LocalWorker
 */
@SuppressWarnings("serial")
public class TavernaRunManager extends UnicastRemoteObject implements
		RemoteRunFactory, RunAccounting, WorkerFactory {
	String command;
	Map<String, String> seedEnvironment = new HashMap<>();
	List<String> javaInitParams = new ArrayList<>();
	private WorkflowBundleIO io;
	private int activeRuns = 0;
	// Hacks!
	public static String interactionHost;
	public static String interactionPort;
	public static String interactionWebdavPath;
	public static String interactionFeedPath;

	/**
	 * How to get the actual workflow document from the XML document that it is
	 * contained in.
	 * 
	 * @param containerDocument
	 *            The document sent from the web interface.
	 * @return The element describing the workflow, as expected by the Taverna
	 *         command line executor.
	 */
	protected Element unwrapWorkflow(Document containerDocument) {
		return (Element) containerDocument.getDocumentElement().getFirstChild();
	}

	private static final String usage = "java -jar server.worker.jar workflowExecScript ?-Ekey=val...? ?-Jconfig? UUID";

	/**
	 * An RMI-enabled factory for runs.
	 * 
	 * @param command
	 *            What command to call to actually run a run.
	 * @throws RemoteException
	 *             If anything goes wrong during creation of the instance.
	 */
	public TavernaRunManager(String command) throws RemoteException {
		this.command = command;
		this.io = new WorkflowBundleIO();
	}

	@Override
	public RemoteSingleRun make(byte[] workflow, String creator,
			UsageRecordReceiver urReceiver, UUID id) throws RemoteException {
		if (creator == null)
			throw new RemoteException("no creator");
		try {
			URI wfid = io.readBundle(new ByteArrayInputStream(workflow), null)
					.getMainWorkflow().getIdentifier();
			out.println("Creating run from workflow <" + wfid + "> for <"
					+ creator + ">");
			return new LocalWorker(command, workflow, urReceiver, id,
					seedEnvironment, javaInitParams, this);
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
		public void run() {
			try {
				Thread.sleep(DEATH_DELAY);
			} catch (InterruptedException e) {
			} finally {
				exit(0);
			}
		}
	}

	private void addArgument(String arg) {
		if (arg.startsWith("-E")) {
			String trimmed = arg.substring(2);
			int idx = trimmed.indexOf('=');
			if (idx > 0) {
				addEnvironmentDefinition(trimmed.substring(0, idx),
						trimmed.substring(idx + 1));
				return;
			}
		} else if (arg.startsWith("-D")) {
			if (arg.indexOf('=') > 0) {
				addJavaParameter(arg);
				return;
			}
		} else if (arg.startsWith("-J")) {
			addJavaParameter(arg.substring(2));
			return;
		}
		throw new IllegalArgumentException("argument \"" + arg
				+ "\" must start with -D, -E or -J; "
				+ "-D and -E must contain a \"=\"");
	}

	/**
	 * @param args
	 *            The arguments from the command line invocation.
	 * @throws Exception
	 *             If we can't connect to the RMI registry, or if we can't read
	 *             the workflow, or if we can't build the worker instance, or
	 *             register it. Also if the arguments are wrong.
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		if (args.length < 2)
			throw new Exception("wrong # args: must be \"" + usage + "\"");
		if (!getProperty(UNSECURE_PROP, "no").equals("yes")) {
			setProperty(SEC_POLICY_PROP, LocalWorker.class.getClassLoader()
					.getResource(SECURITY_POLICY_FILE).toExternalForm());
			setProperty(RMI_HOST_PROP, LOCALHOST);
		}
		setSecurityManager(new RMISecurityManager());
		factoryName = args[args.length - 1];
		TavernaRunManager man = new TavernaRunManager(args[0]);
		for (int i = 1; i < args.length - 1; i++)
			man.addArgument(args[i]);
		registry = getRegistry(LOCALHOST);

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

	@Override
	public synchronized int countOperatingRuns() {
		return (activeRuns < 0 ? 0 : activeRuns);
	}

	@Override
	public synchronized void runStarted() {
		activeRuns++;
	}

	@Override
	public synchronized void runCeased() {
		activeRuns--;
	}

	@Override
	public Worker makeInstance() throws Exception {
		return new WorkerCore(this);
	}
}
