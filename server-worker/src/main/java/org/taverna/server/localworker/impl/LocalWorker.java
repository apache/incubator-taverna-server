/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.taverna.server.localworker.impl.SecurityConstants.HELIO_TOKEN_NAME;
import static org.taverna.server.localworker.impl.SecurityConstants.KEYSTORE_FILE;
import static org.taverna.server.localworker.impl.SecurityConstants.SECURITY_DIR_NAME;
import static org.taverna.server.localworker.impl.SecurityConstants.TRUSTSTORE_FILE;
import static org.taverna.server.localworker.impl.utils.FilenameVerifier.getValidatedFile;
import static org.taverna.server.localworker.remote.RemoteStatus.Finished;
import static org.taverna.server.localworker.remote.RemoteStatus.Initialized;
import static org.taverna.server.localworker.remote.RemoteStatus.Operating;
import static org.taverna.server.localworker.remote.RemoteStatus.Stopped;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.taverna.server.localworker.remote.IllegalStateTransitionException;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteInput;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteSecurityContext;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.server.UsageRecordReceiver;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

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
@SuppressWarnings({ "SE_BAD_FIELD", "SE_NO_SERIALVERSIONID" })
public class LocalWorker extends UnicastRemoteObject implements RemoteSingleRun {
	// ----------------------- CONSTANTS -----------------------

	/**
	 * Subdirectories of the working directory to create by default.
	 */
	private static final String[] dirstomake = { "conf", "externaltool", "lib",
			"logs", "plugins", "repository", "t2-database", "var" };

	/** The name of the default encoding for characters on this machine. */
	public static final String SYSTEM_ENCODING = defaultCharset().name();

	/** Handle to the directory containing the security info. */
	static final File SECURITY_DIR = new File(
			new File(getProperty("user.home")), SECURITY_DIR_NAME);

	// ----------------------- VARIABLES -----------------------

	/**
	 * Magic flag used to turn off problematic code when testing inside CI
	 * environment.
	 */
	static boolean DO_MKDIR = true;

	/** What to use to run a workflow engine. */
	private final String executeWorkflowCommand;
	/** What workflow to run. */
	private final String workflow;
	/** The remote access object for the working directory. */
	private final DirectoryDelegate baseDir;
	/** What inputs to pass as files. */
	final Map<String, String> inputFiles;
	/** What inputs to pass as files (as file refs). */
	final Map<String, File> inputRealFiles;
	/** What inputs to pass as direct values. */
	final Map<String, String> inputValues;
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
	/** Password to use to encrypt security information. */
	char[] keystorePassword = new char[] { 'c', 'h', 'a', 'n', 'g', 'e', 'm',
			'e' };
	/** Additional server-specified environment settings. */
	Map<String, String> environment = new HashMap<String, String>();

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
	 * @throws RemoteException
	 *             If registration of the worker fails.
	 * @throws ImplementationException
	 *             If something goes wrong during local setup.
	 */
	protected LocalWorker(String executeWorkflowCommand, String workflow,
			Class<? extends Worker> workerClass, UsageRecordReceiver urReceiver)
			throws RemoteException, ImplementationException {
		super();
		masterToken = randomUUID().toString();
		this.workflow = workflow;
		this.executeWorkflowCommand = executeWorkflowCommand;
		base = new File(getProperty("java.io.tmpdir"), masterToken);
		out.println("about to create " + base);
		try {
			forceMkdir(base);
			for (String subdir : dirstomake) {
				new File(base, subdir).mkdir();
			}
		} catch (IOException e) {
			throw new ImplementationException(
					"problem creating run working directory", e);
		}
		baseDir = new DirectoryDelegate(base, null);
		inputFiles = new HashMap<String, String>();
		inputRealFiles = new HashMap<String, File>();
		inputValues = new HashMap<String, String>();
		try {
			core = workerClass.newInstance();
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
				} catch (RemoteException e) {
				}
			}
		});
		getRuntime().addShutdownHook(t);
		shutdownHook = t;
		status = Initialized;
	}

	@Override
	public void destroy() throws RemoteException, ImplementationException {
		if (status != Finished && status != Initialized)
			try {
				core.killWorker();
				if (finish == null)
					finish = new Date();
			} catch (Exception e) {
				out.println("problem when killing worker");
				e.printStackTrace(out);
			}
		try {
			if (shutdownHook != null)
				getRuntime().removeShutdownHook(shutdownHook);
		} catch (RuntimeException e) {
			throw new ImplementationException("problem removing shutdownHook",
					e);
		} finally {
			shutdownHook = null;
		}
		// Is this it?
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
		ArrayList<RemoteInput> result = new ArrayList<RemoteInput>();
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

	@SuppressWarnings("SE_INNER_CLASS")
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
		public void setUriToAliasMap(HashMap<URI, String> uriToAliasMap)
				throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			if (uriToAliasMap == null)
				return;
			ArrayList<String> lines = new ArrayList<String>();
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

	@SuppressWarnings("SE_INNER_CLASS")
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
	public void setStatus(RemoteStatus newStatus)
			throws IllegalStateTransitionException, RemoteException,
			ImplementationException {
		if (status == newStatus)
			return;

		switch (newStatus) {
		case Initialized:
			throw new IllegalStateTransitionException(
					"may not move back to start");
		case Operating:
			switch (status) {
			case Initialized:
				try {
					start = new Date();
					core.initWorker(executeWorkflowCommand, workflow, base,
							inputBaclavaFile, inputRealFiles, inputValues,
							outputBaclavaFile, securityDirectory,
							keystorePassword, environment);
					keystorePassword = null;
				} catch (Exception e) {
					throw new ImplementationException(
							"problem creating executing workflow", e);
				}
				break;
			case Stopped:
				try {
					core.startWorker();
				} catch (Exception e) {
					throw new ImplementationException(
							"problem starting workflow run", e);
				}
				break;
			case Finished:
				throw new IllegalStateTransitionException("already finished");
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
				break;
			}
			status = Finished;
			break;
		}
	}

	@Override
	public Date getFinishTimestamp() {
		return finish == null ? null : new Date(finish.getTime());
	}

	@Override
	public Date getStartTimestamp() {
		return start == null ? null : new Date(start.getTime());
	}
}
