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
import static org.taverna.server.localworker.impl.Constants.DEATH_DELAY;
import static org.taverna.server.localworker.impl.Constants.LOCALHOST;
import static org.taverna.server.localworker.impl.Constants.RMI_HOST_PROP;
import static org.taverna.server.localworker.impl.Constants.SECURITY_POLICY_FILE;
import static org.taverna.server.localworker.impl.Constants.SEC_POLICY_PROP;
import static org.taverna.server.localworker.impl.Constants.UNSECURE_PROP;

import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Holder;

import org.taverna.server.localworker.remote.RemoteRunFactory;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.server.UsageRecordReceiver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
		RemoteRunFactory, RunAccounting, WorkerFactory {
	DocumentBuilderFactory dbf;
	TransformerFactory tf;
	String command;
	// Hacks!
	public static String interactionHost;
	public static String interactionPort;
	public static String interactionWebdavPath;
	public static String interactionFeedPath;
	Map<String, String> seedEnvironment = new HashMap<String, String>();
	List<String> javaInitParams = new ArrayList<String>();
	private int activeRuns = 0;

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
		this.dbf = DocumentBuilderFactory.newInstance();
		this.dbf.setNamespaceAware(true);
		this.dbf.setCoalescing(true);
		this.tf = TransformerFactory.newInstance();
	}

	/**
	 * Do the unwrapping of a workflow to extract the contents of the file to
	 * feed into the Taverna core.
	 * 
	 * @param workflow
	 *            The string containing the workflow to extract.
	 * @param wfid
	 *            A place to store the extracted workflow ID.
	 * @return The extracted workflow description.
	 * @throws RemoteException
	 *             If anything goes wrong.
	 */
	@SuppressWarnings("REC_CATCH_EXCEPTION")
	private String unwrapWorkflow(String workflow, Holder<String> wfid)
			throws RemoteException {
		StringReader sr = new StringReader(workflow);
		StringWriter sw = new StringWriter();
		try {
			Document doc = dbf.newDocumentBuilder().parse(new InputSource(sr));
			// Try to extract the t2flow's ID.
			NodeList nl = doc.getElementsByTagNameNS(
					"http://taverna.sf.net/2008/xml/t2flow", "dataflow");
			if (nl.getLength() > 0) {
				Node n = nl.item(0).getAttributes().getNamedItem("id");
				if (n != null)
					wfid.value = n.getTextContent();
			}
			tf.newTransformer().transform(new DOMSource(unwrapWorkflow(doc)),
					new StreamResult(sw));
			return sw.toString();
		} catch (Exception e) {
			throw new RemoteException("failed to extract contained workflow", e);
		}
	}

	@Override
	public RemoteSingleRun make(String workflow, String creator,
			UsageRecordReceiver urReceiver, UUID id) throws RemoteException {
		if (creator == null)
			throw new RemoteException("no creator");
		try {
			Holder<String> wfid = new Holder<String>("???");
			workflow = unwrapWorkflow(workflow, wfid);
			out.println("Creating run from workflow <" + wfid.value + "> for <"
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
		@SuppressWarnings("DM_EXIT")
		public void run() {
			try {
				Thread.sleep(DEATH_DELAY);
			} catch (InterruptedException e) {
			} finally {
				exit(0);
			}
		}
	}

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
			setProperty(RMI_HOST_PROP, LOCALHOST);
		}
		setSecurityManager(new RMISecurityManager());
		String command = args[0];
		factoryName = args[args.length - 1];
		registry = getRegistry();
		TavernaRunManager man = new TavernaRunManager(command);
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
			throw new IllegalArgumentException(
					"argument \""
							+ args[i]
							+ "\" must start with -D, -E or -J; -D and -E must contain a \"=\"");
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
