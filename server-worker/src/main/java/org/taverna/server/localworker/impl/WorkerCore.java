/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.io.File.createTempFile;
import static java.io.File.pathSeparator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.out;
import static java.net.InetAddress.getLocalHost;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.IOUtils.copy;
import static org.taverna.server.localworker.api.Constants.CREDENTIAL_MANAGER_DIRECTORY;
import static org.taverna.server.localworker.api.Constants.CREDENTIAL_MANAGER_PASSWORD;
import static org.taverna.server.localworker.api.Constants.DEATH_TIME;
import static org.taverna.server.localworker.api.Constants.DEFAULT_LISTENER_NAME;
import static org.taverna.server.localworker.api.Constants.KEYSTORE_PASSWORD;
import static org.taverna.server.localworker.api.Constants.START_WAIT_TIME;
import static org.taverna.server.localworker.api.Constants.SYSTEM_ENCODING;
import static org.taverna.server.localworker.api.Constants.TIME;
import static org.taverna.server.localworker.impl.Status.Aborted;
import static org.taverna.server.localworker.impl.Status.Completed;
import static org.taverna.server.localworker.impl.Status.Failed;
import static org.taverna.server.localworker.impl.Status.Held;
import static org.taverna.server.localworker.impl.Status.Started;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionFeedPath;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionHost;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionPort;
import static org.taverna.server.localworker.impl.TavernaRunManager.interactionWebdavPath;
import static org.taverna.server.localworker.impl.WorkerCore.pmap;
import static org.taverna.server.localworker.remote.RemoteStatus.Finished;
import static org.taverna.server.localworker.remote.RemoteStatus.Initialized;
import static org.taverna.server.localworker.remote.RemoteStatus.Operating;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.ws.Holder;

import org.ogf.usage.JobUsageRecord;
import org.taverna.server.localworker.api.RunAccounting;
import org.taverna.server.localworker.api.Worker;
import org.taverna.server.localworker.impl.utils.TimingOutTask;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.server.UsageRecordReceiver;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
	@NonNull
	static final Map<String, Property> pmap = new HashMap<String, Property>();
	/**
	 * Regular expression to extract the detailed timing information from the
	 * output of /usr/bin/time
	 */
	@NonNull
	private static final Pattern TimeRE;
	static {
		final String TIMERE = "([0-9.:]+)";
		final String TERMS = "(real|user|system|sys|elapsed)";
		TimeRE = Pattern.compile(TIMERE + " *" + TERMS + "[ \t]*" + TIMERE
				+ " *" + TERMS + "[ \t]*" + TIMERE + " *" + TERMS);
	}

	/**
	 * Environment variables to remove before any fork (because they're large or
	 * potentially leaky).
	 */
	// TODO Conduct a proper survey of what to remove
	@NonNull
	private static final String[] ENVIRONMENT_TO_REMOVE = { "SUDO_COMMAND",
			"SUDO_USER", "SUDO_GID", "SUDO_UID", "DISPLAY", "LS_COLORS",
			"XFILESEARCHPATH", "SSH_AGENT_PID", "SSH_AUTH_SOCK" };

	@Nullable
	Process subprocess;
	@NonNull
	final StringWriter stdout;
	@NonNull
	final StringWriter stderr;
	@Nullable
	Integer exitCode;
	boolean readyToSendEmail;
	@Nullable
	String emailAddress;
	@Nullable
	Date start;
	@NonNull
	final RunAccounting accounting;
	@NonNull
	final Holder<Integer> pid;

	private boolean finished;
	@Nullable
	private JobUsageRecord ur;
	@Nullable
	private File wd;
	@Nullable
	private UsageRecordReceiver urreceiver;
	@Nullable
	private File workflowFile;

	/**
	 * @param accounting
	 *            Object that looks after how many runs are executing.
	 * @throws RemoteException
	 */
	public WorkerCore(@NonNull RunAccounting accounting) throws RemoteException {
		super();
		stdout = new StringWriter();
		stderr = new StringWriter();
		pid = new Holder<Integer>();
		this.accounting = accounting;
	}

	private int getPID() {
		synchronized (pid) {
			if (pid.value == null)
				return -1;
			return pid.value;
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
	 * @return <tt>true</tt> if the worker started, or <tt>false</tt> if a
	 *         timeout occurred.
	 * @throws IOException
	 *             If any of quite a large number of things goes wrong.
	 */
	@Override
	public boolean initWorker(@NonNull final LocalWorker local,
			@NonNull final String executeWorkflowCommand,
			@NonNull final String workflow, @NonNull final File workingDir,
			@Nullable final File inputBaclava,
			@NonNull final Map<String, File> inputFiles,
			@NonNull final Map<String, String> inputValues,
			@Nullable final File outputBaclava,
			@NonNull final File securityDir, @Nullable final char[] password,
			@NonNull final Map<String, String> environment,
			@NonNull final String token, @NonNull final List<String> runtime)
			throws IOException {
		try {
			new TimingOutTask() {
				@Override
				public void doIt() throws IOException {
					startExecutorSubprocess(
							createProcessBuilder(local, executeWorkflowCommand,
									workflow, workingDir, inputBaclava,
									inputFiles, inputValues, outputBaclava,
									securityDir, password, environment, token,
									runtime), password);
				}
			}.doOrTimeOut(START_WAIT_TIME);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
		return subprocess != null;
	}

	private void startExecutorSubprocess(@NonNull ProcessBuilder pb,
			@Nullable char[] password) throws IOException {
		// Start the subprocess
		out.println("starting " + pb.command() + " in directory "
				+ pb.directory() + " with environment " + pb.environment());
		subprocess = pb.start();
		if (subprocess == null)
			throw new IOException("unknown failure creating process");
		start = new Date();
		accounting.runStarted();

		// Capture its stdout and stderr
		new AsyncCopy(subprocess.getInputStream(), stdout, pid);
		new AsyncCopy(subprocess.getErrorStream(), stderr);
		if (password != null)
			new PasswordWriterThread(subprocess, password);
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
	@NonNull
	ProcessBuilder createProcessBuilder(@NonNull LocalWorker local,
			@NonNull String executeWorkflowCommand, @NonNull String workflow,
			@NonNull File workingDir, @Nullable File inputBaclava,
			@NonNull Map<String, File> inputFiles,
			@NonNull Map<String, String> inputValues,
			@Nullable File outputBaclava, @NonNull File securityDir,
			@NonNull char[] password, @NonNull Map<String, String> environment,
			@NonNull String token, @NonNull List<String> runtime)
			throws IOException, UnsupportedEncodingException,
			FileNotFoundException {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command().add(TIME);
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
			// Provenance cannot be supported when using baclava output
		} else {
			File out = new File(workingDir, "out");
			if (!out.mkdir())
				throw new IOException("failed to make output directory \"out\"");
			// Taverna needs the dir to *not* exist now
			forceDelete(out);
			pb.command().add("-outputdir");
			pb.command().add(out.getAbsolutePath());
			// Enable provenance generation
			pb.command().add("-embedded");
			pb.command().add("-provenance");
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
		for (String name : ENVIRONMENT_TO_REMOVE)
			env.remove(name);

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
			env.put("INTERACTION_HOST", makeInterHost(local.interactionFeedURL));
			env.put("INTERACTION_PORT", makeInterPort(local.interactionFeedURL));
			env.put("INTERACTION_FEED", makeInterPath(local.interactionFeedURL));
			env.put("INTERACTION_WEBDAV",
					local.webdavURL != null ? local.webdavURL.getPath()
							: interactionWebdavPath);
		}
		return pb;
	}

	@Nullable
	private static String makeInterHost(@Nullable URL url) {
		if (url == null)
			return interactionHost;
		return url.getProtocol() + "://" + url.getHost();
	}

	@Nullable
	private static String makeInterPort(@Nullable URL url) {
		if (url == null)
			return interactionPort;
		int port = url.getPort();
		if (port == -1)
			port = url.getDefaultPort();
		return Integer.toString(port);
	}

	@Nullable
	private static String makeInterPath(@Nullable URL url) {
		if (url == null)
			return interactionFeedPath;
		return url.getPath();
	}

	/**
	 * Kills off the subprocess if it exists and is alive.
	 */
	@Override
	public void killWorker() {
		if (!finished && subprocess != null) {
			final Holder<Integer> code = new Holder<Integer>();
			for (TimingOutTask tot : new TimingOutTask[] { new TimingOutTask() {
				/** Check if the workflow terminated of its own accord */
				@Override
				public void doIt() throws IOException {
					code.value = subprocess.exitValue();
					accounting.runCeased();
					buildUR(code.value == 0 ? Completed : Failed, code.value);
				}
			}, new TimingOutTask() {
				/** Tell the workflow to stop */
				@Override
				public void doIt() throws IOException {
					code.value = killNicely();
					accounting.runCeased();
					buildUR(code.value == 0 ? Completed : Aborted, code.value);
				}
			}, new TimingOutTask() {
				/** Kill the workflow, kill it with fire */
				@Override
				public void doIt() throws IOException {
					code.value = killHard();
					accounting.runCeased();
					buildUR(code.value == 0 ? Completed : Aborted, code.value);
				}
			} }) {
				try {
					tot.doOrTimeOut(DEATH_TIME);
				} catch (Exception e) {
				}
				if (code.value != null)
					break;
			}
			finished = true;
			exitCode = code.value;
			readyToSendEmail = true;
			if (code.value > 128) {
				out.println("workflow aborted, signal=" + (code.value - 128));
			} else {
				out.println("workflow exited, code=" + code.value);
			}
		}
	}

	@NonNull
	private JobUsageRecord newUR() throws DatatypeConfigurationException {
		try {
			if (wd != null)
				return new JobUsageRecord(wd.getName());
		} catch (RuntimeException e) {
		}
		return new JobUsageRecord("unknown");
	}

	/**
	 * Fills in the accounting information from the exit code and stderr.
	 * 
	 * @param exitCode
	 *            The exit code from the program.
	 */
	private void buildUR(@NonNull Status status, int exitCode) {
		try {
			Date now = new Date();
			long user = -1, sys = -1, real = -1;
			Matcher m = TimeRE.matcher(stderr.toString());
			ur = newUR();
			while (m.find())
				for (int i = 1; i < 6; i += 2)
					if (m.group(i + 1).equals("user"))
						user = parseDuration(m.group(i));
					else if (m.group(i + 1).equals("sys")
							|| m.group(i + 1).equals("system"))
						sys = parseDuration(m.group(i));
					else if (m.group(i + 1).equals("real")
							|| m.group(i + 1).equals("elapsed"))
						real = parseDuration(m.group(i));
			if (user != -1)
				ur.addCpuDuration(user).setUsageType("user");
			if (sys != -1)
				ur.addCpuDuration(sys).setUsageType("system");
			ur.addUser(System.getProperty("user.name"), null);
			ur.addStartAndEnd(start, now);
			if (real != -1)
				ur.addWallDuration(real);
			else
				ur.addWallDuration(now.getTime() - start.getTime());
			ur.setStatus(status.toString());
			ur.addHost(getLocalHost().getHostName());
			ur.addResource("exitcode", Integer.toString(exitCode));
			ur.addDisk(sizeOfDirectory(wd)).setStorageUnit("B");
			if (urreceiver != null)
				urreceiver.acceptUsageRecord(ur.marshal());
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private long parseDuration(@NonNull String durationString) {
		try {
			return (long) (parseDouble(durationString) * 1000);
		} catch (NumberFormatException nfe) {
			// Not a double; maybe MM:SS.mm or HH:MM:SS.mm
		}
		long dur = 0;
		for (String d : durationString.split(":"))
			try {
				dur = 60 * dur + parseLong(d);
			} catch (NumberFormatException nfe) {
				// Assume that only one thing is fractional, and that it is last
				return 60000 * dur + (long) (parseDouble(d) * 1000);
			}
		return dur * 1000;
	}

	private void signal(@NonNull String signal) throws Exception {
		int pid = getPID();
		if (pid > 0
				&& getRuntime().exec("kill -" + signal + " " + pid).waitFor() == 0)
			return;
		throw new Exception("failed to send signal " + signal + " to process "
				+ pid);
	}

	@Nullable
	private Integer killNicely() {
		try {
			signal("TERM");
			return subprocess.waitFor();
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	private Integer killHard() {
		try {
			signal("QUIT");
			return subprocess.waitFor();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Move the worker out of the stopped state and back to operating.
	 * 
	 * @throws Exception
	 *             if it fails.
	 */
	@Override
	public void startWorker() throws Exception {
		signal("CONT");
	}

	/**
	 * Move the worker into the stopped state from the operating state.
	 * 
	 * @throws Exception
	 *             if it fails.
	 */
	@Override
	public void stopWorker() throws Exception {
		signal("STOP");
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
			buildUR(exitCode.intValue() == 0 ? Completed : Failed, exitCode);
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
				/*
				 * Note that this record is not to be pushed to the server. That
				 * is done elsewhere (when a proper record is produced)
				 */
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
	public void setURReceiver(@NonNull UsageRecordReceiver receiver) {
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

/**
 * An engine for asynchronously copying from an {@link InputStream} to a
 * {@link Writer}.
 * 
 * @author Donal Fellows
 */
class AsyncCopy extends Thread {
	@NonNull
	private BufferedReader from;
	@NonNull
	private Writer to;
	@Nullable
	private Holder<Integer> pidHolder;

	AsyncCopy(@NonNull InputStream from, @NonNull Writer to)
			throws UnsupportedEncodingException {
		this(from, to, null);
	}

	AsyncCopy(@NonNull InputStream from, @NonNull Writer to,
			@Nullable Holder<Integer> pid) throws UnsupportedEncodingException {
		this.from = new BufferedReader(new InputStreamReader(from,
				SYSTEM_ENCODING));
		this.to = to;
		this.pidHolder = pid;
		setDaemon(true);
		start();
	}

	@Override
	public void run() {
		try {
			if (pidHolder != null) {
				String line = from.readLine();
				if (line.matches("^pid:\\d+$"))
					synchronized (pidHolder) {
						pidHolder.value = parseInt(line.substring(4));
					}
				else
					to.write(line + System.getProperty("line.separator"));
			}
			copy(from, to);
		} catch (IOException e) {
		}
	}
}

/**
 * A helper for asynchronously writing a password to a subprocess's stdin.
 * 
 * @author Donal Fellows
 */
class PasswordWriterThread extends Thread {
	private OutputStream to;
	private char[] chars;

	PasswordWriterThread(@NonNull Process to, @NonNull char[] chars) {
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
			pw = new PrintWriter(new OutputStreamWriter(to, SYSTEM_ENCODING));
			pw.println(chars);
		} catch (UnsupportedEncodingException e) {
			// Not much we can do here
			e.printStackTrace();
		} finally {
			/*
			 * We don't trust GC to clear password from memory. We also take
			 * care not to clear the default password!
			 */
			if (chars != KEYSTORE_PASSWORD)
				Arrays.fill(chars, '\00');
			if (pw != null)
				pw.close();
		}
	}
}

enum Property {
	STDOUT("stdout"), STDERR("stderr"), EXIT_CODE("exitcode"), READY_TO_NOTIFY(
			"readyToNotify"), EMAIL("notificationAddress"), USAGE("usageRecord");

	private String s;

	private Property(String s) {
		this.s = s;
		pmap.put(s, this);
	}

	@Override
	public String toString() {
		return s;
	}

	public static Property is(@NonNull String s) {
		return pmap.get(s);
	}

	@NonNull
	public static String[] names() {
		return pmap.keySet().toArray(new String[pmap.size()]);
	}
}

enum Status {
	Aborted, Completed, Failed, Held, Queued, Started, Suspended
}
