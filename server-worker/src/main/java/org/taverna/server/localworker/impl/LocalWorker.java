/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.taverna.server.localworker.api.Constants.HELIO_TOKEN_NAME;
import static org.taverna.server.localworker.api.Constants.KEYSTORE_FILE;
import static org.taverna.server.localworker.api.Constants.KEYSTORE_PASSWORD;
import static org.taverna.server.localworker.api.Constants.SECURITY_DIR_NAME;
import static org.taverna.server.localworker.api.Constants.SHARED_DIR_PROP;
import static org.taverna.server.localworker.api.Constants.SUBDIR_LIST;
import static org.taverna.server.localworker.api.Constants.SYSTEM_ENCODING;
import static org.taverna.server.localworker.api.Constants.TRUSTSTORE_FILE;
import static org.taverna.server.localworker.impl.utils.FilenameVerifier.getValidatedFile;
import static org.taverna.server.localworker.remote.RemoteStatus.Finished;
import static org.taverna.server.localworker.remote.RemoteStatus.Initialized;
import static org.taverna.server.localworker.remote.RemoteStatus.Operating;
import static org.taverna.server.localworker.remote.RemoteStatus.Stopped;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.taverna.server.localworker.api.Worker;
import org.taverna.server.localworker.api.WorkerFactory;
import org.taverna.server.localworker.remote.IllegalStateTransitionException;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteInput;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteSecurityContext;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.remote.StillWorkingOnItException;
import org.taverna.server.localworker.server.UsageRecordReceiver;

/**
 * This class implements one side of the connection between the Taverna Server
 * master server and this process. It delegates to a {@link Worker} instance the
 * handling of actually running a workflow.
 * 
 * @author Donal Fellows
 * @see DirectoryDelegate
 * @see FileDelegate
 * @see WorkerCore
 */
@SuppressWarnings("serial")
public class LocalWorker extends UnicastRemoteObject implements RemoteSingleRun {
	// ----------------------- CONSTANTS -----------------------

	/** Handle to the directory containing the security info. */
	static final File SECURITY_DIR;
	static final String SLASHTEMP;
	static {
		SLASHTEMP = getProperty("java.io.tmpdir");
		File home = new File(getProperty("user.home"));
		// If we can't write to $HOME (i.e., we're in an odd deployment) use
		// the official version of /tmp/$PID as a fallback.
		if (!home.canWrite())
			home = new File(SLASHTEMP, getRuntimeMXBean().getName());
		SECURITY_DIR = new File(home, SECURITY_DIR_NAME);
	}

	// ----------------------- VARIABLES -----------------------

	/**
	 * Magic flag used to turn off problematic code when testing inside CI
	 * environment.
	 */
	static boolean DO_MKDIR = true;

	/** What to use to run a workflow engine. */
	private final String executeWorkflowCommand;
	/** What workflow to run. */
	private final byte[] workflow;
	/** The remote access object for the working directory. */
	private final DirectoryDelegate baseDir;
	/** What inputs to pass as files. */
	final Map<String, String> inputFiles;
	/** What inputs to pass as files (as file refs). */
	final Map<String, File> inputRealFiles;
	/** What inputs to pass as direct values. */
	final Map<String, String> inputValues;
	/** What delimiters to use. */
	final Map<String, String> inputDelimiters;
	/** The interface to the workflow engine subprocess. */
	private final Worker core;
	/** Our descriptor token (UUID). */
	private final String masterToken;
	/**
	 * The root working directory for a workflow run, or <tt>null</tt> if it has
	 * been deleted.
	 */
	private File base;
	/**
	 * When did this workflow start running, or <tt>null</tt> for
	 * "never/not yet".
	 */
	private Date start;
	/**
	 * When did this workflow finish running, or <tt>null</tt> for
	 * "never/not yet".
	 */
	private Date finish;
	/** The cached status of the workflow run. */
	RemoteStatus status;
	/**
	 * The name of the input Baclava document, or <tt>null</tt> to not do it
	 * that way.
	 */
	String inputBaclava;
	/**
	 * The name of the output Baclava document, or <tt>null</tt> to not do it
	 * that way.
	 */
	String outputBaclava;
	/**
	 * The file containing the input Baclava document, or <tt>null</tt> to not
	 * do it that way.
	 */
	private File inputBaclavaFile;
	/**
	 * The file containing the output Baclava document, or <tt>null</tt> to not
	 * do it that way.
	 */
	private File outputBaclavaFile;
	/**
	 * Registered shutdown hook so that we clean up when this process is killed
	 * off, or <tt>null</tt> if that is no longer necessary.
	 */
	Thread shutdownHook;
	/** Location for security information to be written to. */
	File securityDirectory;
	/**
	 * Password to use to encrypt security information.
	 */
	char[] keystorePassword = KEYSTORE_PASSWORD;
	/** Additional server-specified environment settings. */
	Map<String, String> environment = new HashMap<>();
	/** Additional server-specified java runtime settings. */
	List<String> runtimeSettings = new ArrayList<>();
	URL interactionFeedURL;
	URL webdavURL;
	URL publishURL;//FIXME
	private boolean doProvenance = true;

	// ----------------------- METHODS -----------------------

	/**
	 * @param executeWorkflowCommand
	 *            The script used to execute workflows.
	 * @param workflow
	 *            The workflow to execute.
	 * @param workerClass
	 *            The class to instantiate as our local representative of the
	 *            run.
	 * @param urReceiver
	 *            The remote class to report the generated usage record(s) to.
	 * @param id
	 *            The UUID to use, or <tt>null</tt> if we are to invent one.
	 * @param seedEnvironment
	 *            The key/value pairs to seed the worker subprocess environment
	 *            with.
	 * @param javaParams
	 *            Parameters to pass to the worker subprocess java runtime
	 *            itself.
	 * @param workerFactory
	 *            How to make instances of the low-level worker objects.
	 * @throws RemoteException
	 *             If registration of the worker fails.
	 * @throws ImplementationException
	 *             If something goes wrong during local setup.
	 */
	protected LocalWorker(String executeWorkflowCommand, byte[] workflow,
			UsageRecordReceiver urReceiver, UUID id,
			Map<String, String> seedEnvironment, List<String> javaParams,
			WorkerFactory workerFactory) throws RemoteException,
			ImplementationException {
		super();
		if (id == null)
			id = randomUUID();
		masterToken = id.toString();
		this.workflow = workflow;
		this.executeWorkflowCommand = executeWorkflowCommand;
		String sharedDir = getProperty(SHARED_DIR_PROP, SLASHTEMP);
		base = new File(sharedDir, masterToken);
		out.println("about to create " + base);
		try {
			forceMkdir(base);
			for (String subdir : SUBDIR_LIST) {
				new File(base, subdir).mkdir();
			}
		} catch (IOException e) {
			throw new ImplementationException(
					"problem creating run working directory", e);
		}
		baseDir = new DirectoryDelegate(base, null);
		inputFiles = new HashMap<>();
		inputRealFiles = new HashMap<>();
		inputValues = new HashMap<>();
		inputDelimiters = new HashMap<>();
		environment.putAll(seedEnvironment);
		runtimeSettings.addAll(javaParams);
		try {
			core = workerFactory.makeInstance();
		} catch (Exception e) {
			out.println("problem when creating core worker implementation");
			e.printStackTrace(out);
			throw new ImplementationException(
					"problem when creating core worker implementation", e);
		}
		core.setURReceiver(urReceiver);
		Thread t = new Thread(new Runnable() {
			/**
			 * Kill off the worker launched by the core.
			 */
			@Override
			public void run() {
				try {
					shutdownHook = null;
					destroy();
				} catch (ImplementationException e) {
					// Absolutely nothing we can do here
				}
			}
		});
		getRuntime().addShutdownHook(t);
		shutdownHook = t;
		status = Initialized;
	}

	@Override
	public void destroy() throws ImplementationException {
		killWorkflowSubprocess();
		removeFromShutdownHooks();
		// Is this it?
		deleteWorkingDirectory();
		deleteSecurityManagerDirectory();
		core.deleteLocalResources();
	}

	private void killWorkflowSubprocess() {
		if (status != Finished && status != Initialized)
			try {
				core.killWorker();
				if (finish == null)
					finish = new Date();
			} catch (Exception e) {
				out.println("problem when killing worker");
				e.printStackTrace(out);
			}
	}

	private void removeFromShutdownHooks() throws ImplementationException {
		try {
			if (shutdownHook != null)
				getRuntime().removeShutdownHook(shutdownHook);
		} catch (RuntimeException e) {
			throw new ImplementationException("problem removing shutdownHook",
					e);
		} finally {
			shutdownHook = null;
		}
	}

	private void deleteWorkingDirectory() throws ImplementationException {
		try {
			if (base != null)
				forceDelete(base);
		} catch (IOException e) {
			out.println("problem deleting working directory");
			e.printStackTrace(out);
			throw new ImplementationException(
					"problem deleting working directory", e);
		} finally {
			base = null;
		}
	}

	private void deleteSecurityManagerDirectory()
			throws ImplementationException {
		try {
			if (securityDirectory != null)
				forceDelete(securityDirectory);
		} catch (IOException e) {
			out.println("problem deleting security directory");
			e.printStackTrace(out);
			throw new ImplementationException(
					"problem deleting security directory", e);
		} finally {
			securityDirectory = null;
		}
	}

	@Override
	public void addListener(RemoteListener listener) throws RemoteException,
			ImplementationException {
		throw new ImplementationException("not implemented");
	}

	@Override
	public String getInputBaclavaFile() {
		return inputBaclava;
	}

	@Override
	public List<RemoteInput> getInputs() throws RemoteException {
		ArrayList<RemoteInput> result = new ArrayList<>();
		for (String name : inputFiles.keySet())
			result.add(new InputDelegate(name));
		return result;
	}

	@Override
	public List<String> getListenerTypes() {
		return emptyList();
	}

	@Override
	public List<RemoteListener> getListeners() {
		return singletonList(core.getDefaultListener());
	}

	@Override
	public String getOutputBaclavaFile() {
		return outputBaclava;
	}

	class SecurityDelegate extends UnicastRemoteObject implements
			RemoteSecurityContext {
		private void setPrivatePerms(File dir) {
			if (!dir.setReadable(false, false) || !dir.setReadable(true, true)
					|| !dir.setExecutable(false, false)
					|| !dir.setExecutable(true, true)
					|| !dir.setWritable(false, false)
					|| !dir.setWritable(true, true)) {
				out.println("warning: "
						+ "failed to set permissions on security context directory");
			}
		}

		protected SecurityDelegate(String token) throws IOException {
			super();
			if (DO_MKDIR) {
				securityDirectory = new File(SECURITY_DIR, token);
				forceMkdir(securityDirectory);
				setPrivatePerms(securityDirectory);
			}
		}

		/**
		 * Write some data to a given file in the context directory.
		 * 
		 * @param name
		 *            The name of the file to write.
		 * @param data
		 *            The bytes to put in the file.
		 * @throws RemoteException
		 *             If anything goes wrong.
		 * @throws ImplementationException
		 */
		protected void write(String name, byte[] data) throws RemoteException,
				ImplementationException {
			try {
				File f = new File(securityDirectory, name);
				writeByteArrayToFile(f, data);
			} catch (IOException e) {
				throw new ImplementationException("problem writing " + name, e);
			}
		}

		/**
		 * Write some data to a given file in the context directory.
		 * 
		 * @param name
		 *            The name of the file to write.
		 * @param data
		 *            The lines to put in the file. The
		 *            {@linkplain LocalWorker#SYSTEM_ENCODING system encoding}
		 *            will be used to do the writing.
		 * @throws RemoteException
		 *             If anything goes wrong.
		 * @throws ImplementationException
		 */
		protected void write(String name, Collection<String> data)
				throws RemoteException, ImplementationException {
			try {
				File f = new File(securityDirectory, name);
				writeLines(f, SYSTEM_ENCODING, data);
			} catch (IOException e) {
				throw new ImplementationException("problem writing " + name, e);
			}
		}

		/**
		 * Write some data to a given file in the context directory.
		 * 
		 * @param name
		 *            The name of the file to write.
		 * @param data
		 *            The line to put in the file. The
		 *            {@linkplain LocalWorker#SYSTEM_ENCODING system encoding}
		 *            will be used to do the writing.
		 * @throws RemoteException
		 *             If anything goes wrong.
		 * @throws ImplementationException
		 */
		protected void write(String name, char[] data) throws RemoteException,
				ImplementationException {
			try {
				File f = new File(securityDirectory, name);
				writeLines(f, SYSTEM_ENCODING, asList(new String(data)));
			} catch (IOException e) {
				throw new ImplementationException("problem writing " + name, e);
			}
		}

		@Override
		public void setKeystore(byte[] keystore) throws RemoteException,
				ImplementationException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			if (keystore == null)
				throw new IllegalArgumentException("keystore may not be null");
			write(KEYSTORE_FILE, keystore);
		}

		@Override
		public void setPassword(char[] password) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			if (password == null)
				throw new IllegalArgumentException("password may not be null");
			keystorePassword = password.clone();
		}

		@Override
		public void setTruststore(byte[] truststore) throws RemoteException,
				ImplementationException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			if (truststore == null)
				throw new IllegalArgumentException("truststore may not be null");
			write(TRUSTSTORE_FILE, truststore);
		}

		@Override
		public void setUriToAliasMap(Map<URI, String> uriToAliasMap)
				throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			if (uriToAliasMap == null)
				return;
			ArrayList<String> lines = new ArrayList<>();
			for (Entry<URI, String> site : uriToAliasMap.entrySet())
				lines.add(site.getKey().toASCIIString() + " " + site.getValue());
			// write(URI_ALIAS_MAP, lines);
		}

		@Override
		public void setHelioToken(String helioToken) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			out.println("registering HELIO CIS token for export");
			environment.put(HELIO_TOKEN_NAME, helioToken);
		}
	}

	@Override
	public RemoteSecurityContext getSecurityContext() throws RemoteException,
			ImplementationException {
		try {
			return new SecurityDelegate(masterToken);
		} catch (RemoteException e) {
			if (e.getCause() != null)
				throw new ImplementationException(
						"problem initializing security context", e.getCause());
			throw e;
		} catch (IOException e) {
			throw new ImplementationException(
					"problem initializing security context", e);
		}
	}

	@Override
	public RemoteStatus getStatus() {
		// only state that can spontaneously change to another
		if (status == Operating) {
			status = core.getWorkerStatus();
			if (status == Finished && finish == null)
				finish = new Date();
		}
		return status;
	}

	@Override
	public RemoteDirectory getWorkingDirectory() {
		return baseDir;
	}

	File validateFilename(String filename) throws RemoteException {
		if (filename == null)
			throw new IllegalArgumentException("filename must be non-null");
		try {
			return getValidatedFile(base, filename.split("/"));
		} catch (IOException e) {
			throw new IllegalArgumentException("failed to validate filename", e);
		}
	}

	class InputDelegate extends UnicastRemoteObject implements RemoteInput {
		private String name;

		InputDelegate(String name) throws RemoteException {
			super();
			this.name = name;
			if (!inputFiles.containsKey(name)) {
				if (status != Initialized)
					throw new IllegalStateException("not initializing");
				inputFiles.put(name, null);
				inputRealFiles.put(name, null);
				inputValues.put(name, null);
				inputDelimiters.put(name, null);
			}
		}

		@Override
		public String getFile() {
			return inputFiles.get(name);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return inputValues.get(name);
		}

		@Override
		public String getDelimiter() throws RemoteException {
			return inputDelimiters.get(name);
		}

		@Override
		public void setFile(String file) throws RemoteException {
			if (status != Initialized)
				throw new IllegalStateException("not initializing");
			inputRealFiles.put(name, validateFilename(file));
			inputValues.put(name, null);
			inputFiles.put(name, file);
			inputBaclava = null;
		}

		@Override
		public void setValue(String value) throws RemoteException {
			if (status != Initialized)
				throw new IllegalStateException("not initializing");
			inputValues.put(name, value);
			inputFiles.put(name, null);
			inputRealFiles.put(name, null);
			inputBaclava = null;
		}

		@Override
		public void setDelimiter(String delimiter) throws RemoteException {
			if (status != Initialized)
				throw new IllegalStateException("not initializing");
			if (inputBaclava != null)
				throw new IllegalStateException("input baclava file set");
			if (delimiter != null) {
				if (delimiter.length() > 1)
					throw new IllegalStateException(
							"multi-character delimiter not permitted");
				if (delimiter.charAt(0) == 0)
					throw new IllegalStateException(
							"may not use NUL for splitting");
				if (delimiter.charAt(0) > 127)
					throw new IllegalStateException(
							"only ASCII characters supported for splitting");
			}
			inputDelimiters.put(name, delimiter);
		}
	}

	@Override
	public RemoteInput makeInput(String name) throws RemoteException {
		return new InputDelegate(name);
	}

	@Override
	public RemoteListener makeListener(String type, String configuration)
			throws RemoteException {
		throw new RemoteException("listener manufacturing unsupported");
	}

	@Override
	public void setInputBaclavaFile(String filename) throws RemoteException {
		if (status != Initialized)
			throw new IllegalStateException("not initializing");
		inputBaclavaFile = validateFilename(filename);
		for (String input : inputFiles.keySet()) {
			inputFiles.put(input, null);
			inputRealFiles.put(input, null);
			inputValues.put(input, null);
		}
		inputBaclava = filename;
	}

	@Override
	public void setOutputBaclavaFile(String filename) throws RemoteException {
		if (status != Initialized)
			throw new IllegalStateException("not initializing");
		if (filename != null)
			outputBaclavaFile = validateFilename(filename);
		else
			outputBaclavaFile = null;
		outputBaclava = filename;
	}

	@Override
	public void setGenerateProvenance(boolean prov) {
		doProvenance = prov;
	}

	@Override
	public void setStatus(RemoteStatus newStatus)
			throws IllegalStateTransitionException, RemoteException,
			ImplementationException, StillWorkingOnItException {
		if (status == newStatus)
			return;

		switch (newStatus) {
		case Initialized:
			throw new IllegalStateTransitionException(
					"may not move back to start");
		case Operating:
			switch (status) {
			case Initialized:
				boolean started;
				try {
					started = createWorker();
				} catch (Exception e) {
					throw new ImplementationException(
							"problem creating executing workflow", e);
				}
				if (!started)
					throw new StillWorkingOnItException(
							"workflow start in process");
				break;
			case Stopped:
				try {
					core.startWorker();
				} catch (Exception e) {
					throw new ImplementationException(
							"problem continuing workflow run", e);
				}
				break;
			case Finished:
				throw new IllegalStateTransitionException("already finished");
			default:
				break;
			}
			status = Operating;
			break;
		case Stopped:
			switch (status) {
			case Initialized:
				throw new IllegalStateTransitionException(
						"may only stop from Operating");
			case Operating:
				try {
					core.stopWorker();
				} catch (Exception e) {
					throw new ImplementationException(
							"problem stopping workflow run", e);
				}
				break;
			case Finished:
				throw new IllegalStateTransitionException("already finished");
			default:
				break;
			}
			status = Stopped;
			break;
		case Finished:
			switch (status) {
			case Operating:
			case Stopped:
				try {
					core.killWorker();
					if (finish == null)
						finish = new Date();
				} catch (Exception e) {
					throw new ImplementationException(
							"problem killing workflow run", e);
				}
			default:
				break;
			}
			status = Finished;
			break;
		}
	}

	private boolean createWorker() throws Exception {
		start = new Date();
		char[] pw = keystorePassword;
		keystorePassword = null;
		/*
		 * Do not clear the keystorePassword array here; its ownership is
		 * *transferred* to the worker core which doesn't copy it but *does*
		 * clear it after use.
		 */
		return core.initWorker(this, executeWorkflowCommand, workflow, base,
				inputBaclavaFile, inputRealFiles, inputValues, inputDelimiters,
				outputBaclavaFile, securityDirectory, pw, doProvenance,
				environment, masterToken, runtimeSettings);
	}

	@Override
	public Date getFinishTimestamp() {
		return finish == null ? null : new Date(finish.getTime());
	}

	@Override
	public Date getStartTimestamp() {
		return start == null ? null : new Date(start.getTime());
	}

	@Override
	public void setInteractionServiceDetails(URL feed, URL webdav, URL publish) {
		interactionFeedURL = feed;
		webdavURL = webdav;
		publishURL = publish;
	}

	@Override
	public void ping() {
		// Do nothing here; this *should* be empty
	}
}
