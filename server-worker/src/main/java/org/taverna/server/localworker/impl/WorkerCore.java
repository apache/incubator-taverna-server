/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.io.File.createTempFile;
import static java.io.File.pathSeparator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.System.out;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.IOUtils.copy;
import static org.taverna.server.localworker.impl.Constants.CREDENTIAL_MANAGER_DIRECTORY;
import static org.taverna.server.localworker.impl.Constants.CREDENTIAL_MANAGER_PASSWORD;
import static org.taverna.server.localworker.impl.Constants.DEFAULT_LISTENER_NAME;
import static org.taverna.server.localworker.impl.Constants.KEYSTORE_PASSWORD;
import static org.taverna.server.localworker.impl.Constants.START_WAIT_TIME;
import static org.taverna.server.localworker.impl.Constants.SYSTEM_ENCODING;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionFeedPath;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionHost;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionPort;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionWebdavPath;
import static org.taverna.server.localworker.impl.WorkerCore.Status.Aborted;
import static org.taverna.server.localworker.impl.WorkerCore.Status.Completed;
import static org.taverna.server.localworker.impl.WorkerCore.Status.Failed;
import static org.taverna.server.localworker.impl.WorkerCore.Status.Held;
import static org.taverna.server.localworker.impl.WorkerCore.Status.Started;
import static org.taverna.server.localworker.remote.RemoteStatus.Finished;
import static org.taverna.server.localworker.remote.RemoteStatus.Initialized;
import static org.taverna.server.localworker.remote.RemoteStatus.Operating;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.ws.Holder;

import org.ogf.usage.JobUsageRecord;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.server.UsageRecordReceiver;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The core class that connects to a Taverna command-line workflow execution
 * engine. This implementation always registers a single listener, &lquo;
 * <tt>io</tt> &rquo;, with two properties representing the stdout and stderr of
 * the run and one representing the exit code. The listener is
 * remote-accessible. It does not support attaching any other listeners.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings({ "SE_BAD_FIELD", "SE_NO_SERIALVERSIONID" })
@java.lang.SuppressWarnings("serial")
public class WorkerCore extends UnicastRemoteObject implements Worker,
		RemoteListener {
	static final Map<String, Property> pmap = new HashMap<String, Property>();

	enum Property {
		STDOUT("stdout"), STDERR("stderr"), EXIT_CODE("exitcode"), READY_TO_NOTIFY(
				"readyToNotify"), EMAIL("notificationAddress"), USAGE(
				"usageRecord");

		private String s;

		private Property(String s) {
			this.s = s;
			pmap.put(s, this);
		}

		@Override
		public String toString() {
			return s;
		}

		public static Property is(String s) {
			return pmap.get(s);
		}

		public static String[] names() {
			return pmap.keySet().toArray(new String[pmap.size()]);
		}
	}

	enum Status {
		Aborted, Completed, Failed, Held, Queued, Started, Suspended
	}

	Process subprocess;
	StringWriter stdout;
	StringWriter stderr;
	Integer exitCode;
	boolean readyToSendEmail;
	String emailAddress;
	Date start;
	RunAccounting accounting;

	private boolean finished;
	private JobUsageRecord ur;
	private File wd;
	private UsageRecordReceiver urreceiver;
	private File workflowFile;

	/**
	 * @param accounting
	 *            Object that looks after how many runs are executing.
	 * @throws RemoteException
	 */
	public WorkerCore(RunAccounting accounting) throws RemoteException {
		super();
		stdout = new StringWriter();
		stderr = new StringWriter();
		this.accounting = accounting;
	}

	/**
	 * An engine for asynchronously copying from an {@link InputStream} to a
	 * {@link Writer}.
	 * 
	 * @author Donal Fellows
	 */
	private static class AsyncCopy extends Thread {
		private InputStream from;
		private Writer to;

		AsyncCopy(InputStream from, Writer to) {
			this.from = from;
			this.to = to;
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			try {
				copy(from, to, SYSTEM_ENCODING);
			} catch (IOException e) {
			}
		}
	}

	/**
	 * A helper for asynchronously writing a password to a subprocess's stdin.
	 * 
	 * @author Donal Fellows
	 */
	private static class PasswordWriterThread extends Thread {
		private OutputStream to;
		private char[] chars;

		PasswordWriterThread(Process to, char[] chars) {
			this.to = to.getOutputStream();
			assert chars != null;
			this.chars = chars;
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(
						new OutputStreamWriter(to, SYSTEM_ENCODING));
				pw.println(chars);
			} catch (UnsupportedEncodingException e) {
				// Not much we can do here
				e.printStackTrace();
			} finally {
				// We don't trust GC to clear password from memory
				// We also take care not to clear the default password!
				if (chars != KEYSTORE_PASSWORD)
					Arrays.fill(chars, '\00');
				if (pw != null)
					pw.close();
			}
		}

	}

	/**
	 * Fire up the workflow. This causes a transition into the operating state.
	 * 
	 * @param executeWorkflowCommand
	 *            The command to run to execute the workflow.
	 * @param workflow
	 *            The workflow document to execute.
	 * @param workingDir
	 *            What directory to use as the working directory.
	 * @param inputBaclava
	 *            The baclava file to use for inputs, or <tt>null</tt> to use
	 *            the other <b>input*</b> arguments' values.
	 * @param inputFiles
	 *            A mapping of input names to files that supply them. Note that
	 *            we assume that nothing mapped here will be mapped in
	 *            <b>inputValues</b>.
	 * @param inputValues
	 *            A mapping of input names to values to supply to them. Note
	 *            that we assume that nothing mapped here will be mapped in
	 *            <b>inputFiles</b>.
	 * @param outputBaclava
	 *            What baclava file to write the output from the workflow into,
	 *            or <tt>null</tt> to have it written into the <tt>out</tt>
	 *            subdirectory.
	 * @param token
	 *            The name of the workflow run.
	 * @throws IOException
	 *             If any of quite a large number of things goes wrong.
	 */
	@Override
	public boolean initWorker(final LocalWorker local,
			final String executeWorkflowCommand, final String workflow,
			final File workingDir, final File inputBaclava,
			final Map<String, File> inputFiles,
			final Map<String, String> inputValues, final File outputBaclava,
			final File securityDir, final char[] password,
			final Map<String, String> environment, final String token,
			final List<String> runtime) throws IOException {
		final Holder<IOException> h = new Holder<IOException>();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ProcessBuilder pb = createProcessBuilder(local,
							executeWorkflowCommand, workflow, workingDir,
							inputBaclava, inputFiles, inputValues,
							outputBaclava, securityDir, password, environment,
							token, runtime);

					// Start the subprocess
					out.println("starting " + pb.command() + " in directory "
							+ workingDir);
					subprocess = pb.start();
					if (subprocess == null)
						throw new IOException(
								"unknown failure creating process");
					start = new Date();
					accounting.runStarted();

					// Capture its stdout and stderr
					new AsyncCopy(subprocess.getInputStream(), stdout);
					new AsyncCopy(subprocess.getErrorStream(), stderr);
					if (password != null)
						new PasswordWriterThread(subprocess, password);
				} catch (IOException e) {
					h.value = e;
				}
			}
		});
		t.start();
		try {
			t.join(START_WAIT_TIME);
		} catch (InterruptedException e) {
			// Won't happen
		}
		if (h.value != null)
			throw h.value;
		return subprocess != null;
	}

	/**
	 * Assemble the process builder. Does not launch the subprocess.
	 * 
	 * @param local
	 *            The local worker container.
	 * @param executeWorkflowCommand
	 *            The reference to the workflow engine implementation.
	 * @param workflow
	 *            The workflow to execute.
	 * @param workingDir
	 *            The working directory to use.
	 * @param inputBaclava
	 *            What file to read a baclava document from (or <tt>null</tt>)
	 * @param inputFiles
	 *            The mapping from inputs to files.
	 * @param inputValues
	 *            The mapping from inputs to literal values.
	 * @param outputBaclava
	 *            What file to write a baclava document to (or <tt>null</tt>)
	 * @param securityDir
	 *            The credential manager directory.
	 * @param password
	 *            The password for the credential manager.
	 * @param environment
	 *            The seed environment
	 * @param token
	 *            The run identifier that the server wants to use.
	 * @param runtime
	 *            Any runtime parameters to Java.
	 * @return The configured process builder.
	 * @throws IOException
	 *             If file handling fails
	 * @throws UnsupportedEncodingException
	 *             If we can't encode any text (unlikely)
	 * @throws FileNotFoundException
	 *             If we can't write the workflow out (unlikely)
	 */
	ProcessBuilder createProcessBuilder(LocalWorker local,
			String executeWorkflowCommand, String workflow, File workingDir,
			File inputBaclava, Map<String, File> inputFiles,
			Map<String, String> inputValues, File outputBaclava,
			File securityDir, char[] password, Map<String, String> environment,
			String token, List<String> runtime) throws IOException,
			UnsupportedEncodingException, FileNotFoundException {
		ProcessBuilder pb = new ProcessBuilder();
		/*
		 * WARNING! HERE THERE BE DRAGONS! BE CAREFUL HERE!
		 * 
		 * Work around _Maven_ bug with permissions in zip files! The executable
		 * bit is stripped by Maven's handling of file permissions, and there's
		 * no practical way to work around it without massively increasing the
		 * pain in other ways. Only want this on Unix - Windows isn't affected
		 * by this - so we use the file separator as a proxy for whether this is
		 * a true POSIX system. Ugly! Ugly ugly ugly...
		 * 
		 * http://jira.codehaus.org/browse/MASSEMBLY-337 is relevant, but not
		 * the whole story as we don't want to use a non-standard packaging
		 * method as there's a real chance of it going wrong in an unexpected
		 * way then. Other parts of the story are that the executable bit isn't
		 * preserved when unpacking with the dependency plugin, and there's no
		 * way to be sure that the servlet container will preserve the bit
		 * either (as that's probably using a Java-based ZIP engine).
		 */
		if (File.separatorChar == '/')
			pb.command().add("/bin/sh");
		pb.command().add(executeWorkflowCommand);
		if (runtime != null)
			pb.command().addAll(runtime);

		// Enable verbose logging
		pb.command().add("-logfile");
		pb.command().add(
				new File(new File(workingDir, "logs"), "detail.log")
						.getAbsolutePath());

		if (securityDir != null) {
			pb.command().add(CREDENTIAL_MANAGER_DIRECTORY);
			pb.command().add(securityDir.getAbsolutePath());
			out.println("security dir location: " + securityDir);
		}
		if (password != null) {
			pb.command().add(CREDENTIAL_MANAGER_PASSWORD);
			out.println("password of length " + password.length
					+ " will be written to subprocess stdin");
		}

		// Add arguments denoting inputs
		if (inputBaclava != null) {
			pb.command().add("-inputdoc");
			pb.command().add(inputBaclava.getAbsolutePath());
			if (!inputBaclava.exists())
				throw new IOException("input baclava file doesn't exist");
		} else {
			for (Entry<String, File> port : inputFiles.entrySet()) {
				if (port.getValue() == null)
					continue;
				pb.command().add("-inputfile");
				pb.command().add(port.getKey());
				pb.command().add(port.getValue().getAbsolutePath());
				if (!port.getValue().exists())
					throw new IOException("input file for port \"" + port
							+ "\" doesn't exist");
			}
			for (Entry<String, String> port : inputValues.entrySet()) {
				if (port.getValue() == null)
					continue;
				pb.command().add("-inputfile");
				pb.command().add(port.getKey());
				File f = createTempFile(".tav_in_", null, workingDir);
				pb.command().add(f.getAbsolutePath());
				write(f, port.getValue(), "UTF-8");
			}
		}

		// Add arguments denoting outputs
		if (outputBaclava != null) {
			pb.command().add("-outputdoc");
			pb.command().add(outputBaclava.getAbsolutePath());
			if (!outputBaclava.getParentFile().exists())
				throw new IOException(
						"parent directory of output baclava file does not exist");
			if (outputBaclava.exists())
				throw new IOException("output baclava file exists");
		} else {
			File out = new File(workingDir, "out");
			if (!out.mkdir())
				throw new IOException("failed to make output directory \"out\"");
			// Taverna needs the dir to *not* exist now
			forceDelete(out);
			pb.command().add("-outputdir");
			pb.command().add(out.getAbsolutePath());
		}

		// Add an argument holding the workflow
		workflowFile = createTempFile(".wf_", ".t2flow", workingDir);
		write(workflowFile, workflow, "UTF-8");
		if (!workflowFile.exists())
			throw new IOException("failed to instantiate workflow file at "
					+ workflowFile);
		pb.command().add(workflowFile.getAbsolutePath());

		// Indicate what working directory to use
		pb.directory(workingDir);
		wd = workingDir;

		Map<String, String> env = pb.environment();

		// Merge any options we have had imposed on us from outside
		env.putAll(environment);

		// Patch the environment to deal with TAVUTILS-17
		assert env.get("PATH") != null;
		env.put("PATH", new File(System.getProperty("java.home"), "bin")
				+ pathSeparator + env.get("PATH"));
		// Patch the environment to deal with TAVSERV-189
		env.put("RAVEN_APPHOME", workingDir.getCanonicalPath());
		// Patch the environment to deal with TAVSERV-224
		env.put("TAVERNA_RUN_ID", token);
		if (interactionHost != null || local.interactionFeedURL != null
				|| local.webdavURL != null) {
			env.put("INTERACTION_HOST",
					local.interactionFeedURL != null ? local.interactionFeedURL
							.getHost() : interactionHost);
			env.put("INTERACTION_PORT",
					local.interactionFeedURL != null ? Integer
							.toString(local.interactionFeedURL.getPort())
							: interactionPort);
			env.put("INTERACTION_FEED",
					local.interactionFeedURL != null ? local.interactionFeedURL
							.getPath() : interactionFeedPath);
			env.put("INTERACTION_WEBDAV",
					local.webdavURL != null ? local.webdavURL.getPath()
							: interactionWebdavPath);
		}
		return pb;
	}

	/**
	 * Kills off the subprocess if it exists and is alive.
	 */
	@Override
	public void killWorker() {
		if (!finished && subprocess != null) {
			int code;
			try {
				// Check if the workflow terminated of its own accord
				code = subprocess.exitValue();
				accounting.runCeased();
				buildUR(code == 0 ? Completed : Failed);
			} catch (IllegalThreadStateException e) {
				subprocess.destroy();
				try {
					code = subprocess.waitFor();
				} catch (InterruptedException e1) {
					e1.printStackTrace(out); // not expected
					return;
				}
				accounting.runCeased();
				buildUR(code == 0 ? Completed : Aborted);
			}
			finished = true;
			exitCode = code;
			readyToSendEmail = true;
			if (code > 128) {
				out.println("workflow aborted, signal=" + (code - 128));
			} else {
				out.println("workflow exited, code=" + code);
			}
		}
	}

	private JobUsageRecord newUR() throws DatatypeConfigurationException {
		try {
			if (wd != null)
				return new JobUsageRecord(wd.getName());
		} catch (RuntimeException e) {
		}
		return new JobUsageRecord("unknown");
	}

	private void buildUR(Status status) {
		try {
			Date now = new Date();
			ur = newUR();
			ur.addUser(System.getProperty("user.name"), null);
			ur.addStartAndEnd(start, now);
			ur.addWallDuration(now.getTime() - start.getTime());
			ur.setStatus(status.toString());
			ur.addHost(InetAddress.getLocalHost().getHostName());
			if (urreceiver != null)
				urreceiver.acceptUsageRecord(ur.marshal());
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Move the worker out of the stopped state and back to operating.
	 * 
	 * @throws Exception
	 *             if it fails (which it always does; operation currently
	 *             unsupported).
	 */
	@Override
	public void startWorker() throws Exception {
		throw new Exception("starting unsupported");
	}

	/**
	 * Move the worker into the stopped state from the operating state.
	 * 
	 * @throws Exception
	 *             if it fails (which it always does; operation currently
	 *             unsupported).
	 */
	@Override
	public void stopWorker() throws Exception {
		throw new Exception("stopping unsupported");
	}

	/**
	 * @return The status of the workflow run. Note that this can be an
	 *         expensive operation.
	 */
	@Override
	public RemoteStatus getWorkerStatus() {
		if (subprocess == null)
			return Initialized;
		if (finished)
			return Finished;
		try {
			exitCode = subprocess.exitValue();
			finished = true;
			readyToSendEmail = true;
			accounting.runCeased();
			buildUR(exitCode.intValue() == 0 ? Completed : Failed);
			return Finished;
		} catch (IllegalThreadStateException e) {
			return Operating;
		}
	}

	@Override
	public String getConfiguration() {
		return "";
	}

	@Override
	public String getName() {
		return DEFAULT_LISTENER_NAME;
	}

	@Override
	@SuppressWarnings("REC_CATCH_EXCEPTION")
	public String getProperty(String propName) throws RemoteException {
		switch (Property.is(propName)) {
		case STDOUT:
			return stdout.toString();
		case STDERR:
			return stderr.toString();
		case EXIT_CODE:
			return (exitCode == null) ? "" : exitCode.toString();
		case EMAIL:
			return emailAddress;
		case READY_TO_NOTIFY:
			return Boolean.toString(readyToSendEmail);
		case USAGE:
			try {
				JobUsageRecord toReturn;
				if (subprocess == null) {
					toReturn = newUR();
					toReturn.setStatus(Held.toString());
				} else if (ur == null) {
					toReturn = newUR();
					toReturn.setStatus(Started.toString());
					toReturn.addStartAndEnd(start, new Date());
					toReturn.addUser(System.getProperty("user.name"), null);
				} else {
					toReturn = ur;
				}
				// Note that this record is not to be pushed to the server
				// That is done elsewhere (when a proper record is produced)
				return toReturn.marshal();
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		default:
			throw new RemoteException("unknown property");
		}
	}

	@Override
	public String getType() {
		return DEFAULT_LISTENER_NAME;
	}

	@Override
	public String[] listProperties() {
		return Property.names();
	}

	@Override
	public void setProperty(String propName, String value)
			throws RemoteException {
		switch (Property.is(propName)) {
		case EMAIL:
			emailAddress = value;
			return;
		case READY_TO_NOTIFY:
			readyToSendEmail = parseBoolean(value);
			return;
		case STDOUT:
		case STDERR:
		case EXIT_CODE:
		case USAGE:
			throw new RemoteException("property is read only");
		default:
			throw new RemoteException("unknown property");
		}
	}

	@Override
	public RemoteListener getDefaultListener() {
		return this;
	}

	@Override
	public void setURReceiver(UsageRecordReceiver receiver) {
		urreceiver = receiver;
	}

	@Override
	public void deleteLocalResources() throws ImplementationException {
		try {
			if (workflowFile != null && workflowFile.getParentFile().exists())
				forceDelete(workflowFile);
		} catch (IOException e) {
			throw new ImplementationException("problem deleting workflow file",
					e);
		}
	}
}
