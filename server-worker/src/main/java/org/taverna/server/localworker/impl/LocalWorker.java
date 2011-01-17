/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileCleaner.track;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.writeLines;
import static org.apache.commons.io.FileUtils.writeStringToFile;
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

import org.taverna.server.localworker.remote.IllegalStateTransitionException;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteInput;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteSecurityContext;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.remote.RemoteStatus;

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
public class LocalWorker extends UnicastRemoteObject implements RemoteSingleRun {
	private String executeWorkflowCommand;
	private String workflow;
	private File base;
	private DirectoryDelegate baseDir;
	/**
	 * When did this workflow start running, or <tt>null</tt> for
	 * "never/not yet".
	 */
	public Date start;
	/**
	 * When did this workflow finish running, or <tt>null</tt> for
	 * "never/not yet".
	 */
	public Date finish;
	RemoteStatus status;
	String inputBaclava, outputBaclava;
	File inputBaclavaFile, outputBaclavaFile;
	Map<String, String> inputFiles;
	Map<String, File> inputRealFiles;
	Map<String, String> inputValues;
	Worker core;
	final String masterToken;
	private Thread shutdownHook;

	/**
	 * @param executeWorkflowCommand
	 * @param workflow
	 * @param workerClass
	 * @throws RemoteException
	 *             If registration of the worker fails.
	 */
	protected LocalWorker(String executeWorkflowCommand, String workflow,
			Class<? extends Worker> workerClass) throws RemoteException {
		super();
		masterToken = randomUUID().toString();
		this.workflow = workflow;
		this.executeWorkflowCommand = executeWorkflowCommand;
		base = new File(masterToken);
		try {
			forceMkdir(base);
		} catch (IOException e) {
			throw new RemoteException("problem creating run working directory",
					e);
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
			throw new RuntimeException(
					"problem when creating core worker implementation", e);
		}
		Thread t = new Thread(new Runnable() {
			/**
			 * Kill off the worker launched by the core.
			 */
			@Override
			public void run() {
				try {
					destroy();
				} catch (RemoteException e) {
				}
			}
		});
		getRuntime().addShutdownHook(t);
		shutdownHook = t;
		status = Initialized;
	}

	@Override
	public void destroy() throws RemoteException {
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
			throw new RemoteException("problem removing shutdownHook", e);
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
			throw new RemoteException("problem deleting working directory", e);
		} finally {
			base = null;
		}
	}

	@Override
	public void addListener(RemoteListener listener) throws RemoteException {
		throw new RemoteException("not implemented");
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

	public static final String SECURITY_DIR_NAME = ".taverna-server-security";
	public static final String KEYSTORE_FILE = "identity.keystore";
	public static final String KEYSTORE_PASS = "identity.password";
	public static final String TRUSTSTORE_FILE = "truststore.jks";
	public static final String TRUSTSTORE_PASS = "truststore.password";
	public static final String URI_ALIAS_MAP = "urlmap.txt";

	static final File SECURITY_DIR = new File(
			new File(getProperty("java.home")), SECURITY_DIR_NAME);

	class SecurityDelegate extends UnicastRemoteObject implements
			RemoteSecurityContext {
		File contextDirectory;

		protected SecurityDelegate() throws IOException {
			super();
			contextDirectory = new File(SECURITY_DIR, masterToken);
			forceMkdir(contextDirectory);
			contextDirectory.setReadable(true, true);
			contextDirectory.setExecutable(true, true);
			contextDirectory.setWritable(true, true);
			track(contextDirectory, LocalWorker.this);
		}

		private static final String ENC = "UTF-8";

		private void write(String name, byte[] data) throws RemoteException {
			try {
				writeByteArrayToFile(new File(contextDirectory, name), data);
			} catch (IOException e) {
				throw new RemoteException("problem writing " + name, e);
			}
		}

		private void write(String name, Collection<String> data)
				throws RemoteException {
			try {
				writeLines(new File(contextDirectory, name), ENC, data);
			} catch (IOException e) {
				throw new RemoteException("problem writing " + name, e);
			}
		}

		private void write(String name, char[] data) throws RemoteException {
			try {
				writeStringToFile(new File(contextDirectory, name), new String(
						data), ENC);
			} catch (IOException e) {
				throw new RemoteException("problem writing " + name, e);
			}
		}

		@Override
		public void setKeystore(byte[] keystore) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			write(KEYSTORE_FILE, keystore);
		}

		@Override
		public void setKeystorePass(char[] password) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			write(KEYSTORE_PASS, password);
		}

		@Override
		public void setTruststore(byte[] truststore) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			write(TRUSTSTORE_FILE, truststore);
		}

		@Override
		public void setTruststorePass(char[] password) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			write(TRUSTSTORE_PASS, password);
		}

		@Override
		public void setUriToAliasMap(HashMap<URI, String> uriToAliasMap)
				throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			ArrayList<String> lines = new ArrayList<String>();
			for (URI site : uriToAliasMap.keySet())
				lines.add(site.toASCIIString() + " " + uriToAliasMap.get(site));
			write(URI_ALIAS_MAP, lines);
		}
	}

	@Override
	public RemoteSecurityContext getSecurityContext() throws RemoteException {
		try {
			return new SecurityDelegate();
		} catch (RemoteException e) {
			if (e.getCause() != null)
				throw new RemoteException(
						"problem initializing security context", e.getCause());
			throw e;
		} catch (IOException e) {
			throw new RemoteException("problem initializing security context",
					e);
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
			throw new RemoteException("failed to validate filename", e);
		}
	}

	class InputDelegate extends UnicastRemoteObject implements RemoteInput {
		private String name;

		InputDelegate(String name) throws RemoteException {
			super();
			this.name = name;
			if (!inputFiles.containsKey(name)) {
				if (status != Initialized)
					throw new RemoteException("not initializing");
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
				throw new RemoteException("not initializing");
			inputRealFiles.put(name, validateFilename(file));
			inputValues.put(name, null);
			inputFiles.put(name, file);
			inputBaclava = null;
		}

		@Override
		public void setValue(String value) throws RemoteException {
			if (status != Initialized)
				throw new RemoteException("not initializing");
			inputValues.put(name, value);
			inputFiles.put(name, null);
			inputRealFiles.put(name, null);
			LocalWorker.this.inputBaclava = null;
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
			throw new RemoteException("not initializing");
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
			throw new RemoteException("not initializing");
		if (filename != null)
			outputBaclavaFile = validateFilename(filename);
		else
			outputBaclavaFile = null;
		outputBaclava = filename;
	}

	@Override
	public void setStatus(RemoteStatus newStatus)
			throws IllegalStateTransitionException, RemoteException {
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
							outputBaclavaFile);
				} catch (Exception e) {
					throw new RemoteException(
							"problem creating executing workflow", e);
				}
				break;
			case Stopped:
				try {
					core.startWorker();
				} catch (Exception e) {
					throw new RemoteException("problem starting workflow run",
							e);
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
					throw new RemoteException("problem stopping workflow run",
							e);
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
					throw new RemoteException("problem killing workflow run", e);
				}
				break;
			}
			status = Finished;
			break;
		}
	}

	@Override
	public Date getFinishTimestamp() {
		return finish;
	}

	@Override
	public Date getStartTimestamp() throws RemoteException {
		return start;
	}
}
