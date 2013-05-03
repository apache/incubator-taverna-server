/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.io.File.separator;
import static java.lang.System.getProperty;
import static java.rmi.registry.Registry.REGISTRY_PORT;
import static org.taverna.server.master.defaults.Default.EXTRA_ARGUMENTS;
import static org.taverna.server.master.defaults.Default.PASSWORD_FILE;
import static org.taverna.server.master.defaults.Default.RMI_PREFIX;
import static org.taverna.server.master.defaults.Default.RUN_LIFE_MINUTES;
import static org.taverna.server.master.defaults.Default.RUN_OPERATING_LIMIT;
import static org.taverna.server.master.defaults.Default.SECURE_FORK_IMPLEMENTATION_JAR;
import static org.taverna.server.master.defaults.Default.SERVER_WORKER_IMPLEMENTATION_JAR;
import static org.taverna.server.master.defaults.Default.SUBPROCESS_START_POLL_SLEEP;
import static org.taverna.server.master.defaults.Default.SUBPROCESS_START_WAIT;
import static org.taverna.server.master.localworker.PersistedState.KEY;
import static org.taverna.server.master.localworker.PersistedState.makeInstance;

import java.io.File;
import java.io.FilenameFilter;

import javax.annotation.PostConstruct;
import javax.jdo.annotations.PersistenceAware;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.defaults.Default;
import org.taverna.server.master.utils.JDOSupport;

/**
 * The persistent state of a local worker factory.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class LocalWorkerState extends JDOSupport<PersistedState> implements
		LocalWorkerModel {
	public LocalWorkerState() {
		super(PersistedState.class);
	}

	private LocalWorkerState self;

	@Required
	public void setSelf(LocalWorkerState self) {
		this.self = self;
	}

	/** Initial lifetime of runs, in minutes. */
	int defaultLifetime;
	/**
	 * Maximum number of runs to exist at once. Note that this includes when
	 * they are just existing for the purposes of file transfer (
	 * {@link Status#Initialized}/{@link Status#Finished} states).
	 */
	int maxRuns;
	/**
	 * Prefix to use for RMI names.
	 */
	String factoryProcessNamePrefix;
	/**
	 * Full path name of the script used to start running a workflow; normally
	 * expected to be "<i>somewhere/</i><tt>executeWorkflow.sh</tt>".
	 */
	String executeWorkflowScript;
	/** Default value for {@link #executeWorkflowScript}. */
	private transient String defaultExecuteWorkflowScript;
	/**
	 * Full path name of the file containing the password used to launch workers
	 * as other users. The file is normally expected to contain a single line,
	 * the password, and to be thoroughly locked down so only the user running
	 * the server (e.g., "<tt>tomcat</tt>") can read it; it will probably reside
	 * in either the user's home directory or in a system configuration
	 * directory.
	 */
	String passwordFile;
	/** Default value for {@link #passwordFile}. */
	private transient String defaultPasswordFile = PASSWORD_FILE;
	/**
	 * The extra arguments to pass to the subprocess.
	 */
	String[] extraArgs;
	/**
	 * How long to wait for subprocess startup, in seconds.
	 */
	int waitSeconds;
	/**
	 * Polling interval to use during startup, in milliseconds.
	 */
	int sleepMS;
	/**
	 * Full path name to the worker process's implementation JAR.
	 */
	String serverWorkerJar;
	private static final String DEFAULT_WORKER_JAR = LocalWorkerState.class
			.getClassLoader().getResource(SERVER_WORKER_IMPLEMENTATION_JAR)
			.getFile();
	/**
	 * Full path name to the Java binary to use to run the subprocess.
	 */
	String javaBinary;
	private static final String DEFAULT_JAVA_BINARY = getProperty("java.home")
			+ separator + "bin" + separator + "java";
	/**
	 * Full path name to the secure fork process's implementation JAR.
	 */
	String serverForkerJar;
	private static final String DEFAULT_FORKER_JAR = LocalWorkerState.class
			.getClassLoader().getResource(SECURE_FORK_IMPLEMENTATION_JAR)
			.getFile();

	String registryHost;
	int registryPort;

	int operatingLimit;

	@Override
	public void setDefaultLifetime(int defaultLifetime) {
		this.defaultLifetime = defaultLifetime;
		if (loadedState)
			self.store();
	}

	@Override
	public int getDefaultLifetime() {
		return defaultLifetime < 1 ? RUN_LIFE_MINUTES : defaultLifetime;
	}

	@Override
	public void setMaxRuns(int maxRuns) {
		this.maxRuns = maxRuns;
		if (loadedState)
			self.store();
	}

	@Override
	public int getMaxRuns() {
		return maxRuns < 1 ? Default.RUN_COUNT_MAX : maxRuns;
	}

	@Override
	public int getOperatingLimit() {
		return operatingLimit < 1 ? RUN_OPERATING_LIMIT : operatingLimit;
	}

	@Override
	public void setOperatingLimit(int operatingLimit) {
		this.operatingLimit = operatingLimit;
		if (loadedState)
			self.store();
	}

	@Override
	public void setFactoryProcessNamePrefix(String factoryProcessNamePrefix) {
		this.factoryProcessNamePrefix = factoryProcessNamePrefix;
		if (loadedState)
			self.store();
	}

	@Override
	public String getFactoryProcessNamePrefix() {
		return factoryProcessNamePrefix == null ? RMI_PREFIX
				: factoryProcessNamePrefix;
	}

	@Override
	public void setExecuteWorkflowScript(String executeWorkflowScript) {
		this.executeWorkflowScript = executeWorkflowScript;
		if (loadedState)
			self.store();
	}

	@Override
	public String getExecuteWorkflowScript() {
		return executeWorkflowScript == null ? defaultExecuteWorkflowScript
				: executeWorkflowScript;
	}

	private static String guessWorkflowScript() {
		File utilDir = new File(DEFAULT_WORKER_JAR).getParentFile();
		File[] dirs = utilDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("taverna-commandline-");
			}
		});
		assert dirs.length > 0;
		return new File(dirs[0], "executeworkflow.sh").toString();
	}

	/**
	 * Set what executeworkflow script to use by default. This is the value that
	 * is used if not overridden by the administration interface.
	 * 
	 * @param defaultScript
	 *            Full path to the script to use.
	 */
	public void setDefaultExecuteWorkflowScript(String defaultScript) {
		if (defaultScript.startsWith("${")) {
			this.defaultExecuteWorkflowScript = guessWorkflowScript();
			return;
		}
		this.defaultExecuteWorkflowScript = defaultScript;
	}

	String getDefaultExecuteWorkflowScript() {
		return defaultExecuteWorkflowScript;
	}

	@Override
	public void setExtraArgs(String[] extraArgs) {
		this.extraArgs = extraArgs.clone();
		if (loadedState)
			self.store();
	}

	@Override
	public String[] getExtraArgs() {
		return extraArgs == null ? EXTRA_ARGUMENTS : extraArgs.clone();
	}

	@Override
	public void setWaitSeconds(int waitSeconds) {
		this.waitSeconds = waitSeconds;
		if (loadedState)
			self.store();
	}

	@Override
	public int getWaitSeconds() {
		return waitSeconds < 1 ? SUBPROCESS_START_WAIT : waitSeconds;
	}

	@Override
	public void setSleepMS(int sleepMS) {
		this.sleepMS = sleepMS;
		if (loadedState)
			self.store();
	}

	@Override
	public int getSleepMS() {
		return sleepMS < 1 ? SUBPROCESS_START_POLL_SLEEP : sleepMS;
	}

	@Override
	public void setServerWorkerJar(String serverWorkerJar) {
		this.serverWorkerJar = serverWorkerJar;
		if (loadedState)
			self.store();
	}

	@Override
	public String getServerWorkerJar() {
		return serverWorkerJar == null ? DEFAULT_WORKER_JAR : serverWorkerJar;
	}

	@Override
	public void setServerForkerJar(String serverForkerJar) {
		this.serverForkerJar = serverForkerJar;
		if (loadedState)
			self.store();
	}

	@Override
	public String getServerForkerJar() {
		return serverForkerJar == null ? DEFAULT_FORKER_JAR : serverForkerJar;
	}

	@Override
	public void setJavaBinary(String javaBinary) {
		this.javaBinary = javaBinary;
		if (loadedState)
			self.store();
	}

	@Override
	public String getJavaBinary() {
		return javaBinary == null ? DEFAULT_JAVA_BINARY : javaBinary;
	}

	@Override
	public void setPasswordFile(String passwordFile) {
		this.passwordFile = passwordFile;
		if (loadedState)
			self.store();
	}

	@Override
	public String getPasswordFile() {
		return passwordFile == null ? defaultPasswordFile : passwordFile;
	}

	void setDefaultPasswordFile(String defaultPasswordFile) {
		this.defaultPasswordFile = defaultPasswordFile;
	}

	@Override
	public void setRegistryHost(String registryHost) {
		this.registryHost = (registryHost == null ? "" : registryHost);
		if (loadedState)
			self.store();
	}

	@Override
	public String getRegistryHost() {
		return (registryHost == null || registryHost.isEmpty()) ? null
				: registryHost;
	}

	@Override
	public void setRegistryPort(int registryPort) {
		this.registryPort = ((registryPort < 1 || registryPort > 65534) ? REGISTRY_PORT
				: registryPort);
		if (loadedState)
			self.store();
	}

	@Override
	public int getRegistryPort() {
		return registryPort == 0 ? REGISTRY_PORT : registryPort;
	}

	// --------------------------------------------------------------

	private boolean loadedState;

	@PostConstruct
	@WithinSingleTransaction
	public void load() {
		if (loadedState || !isPersistent())
			return;
		LocalWorkerModel state = getById(KEY);
		if (state == null) {
			store();
			return;
		}

		defaultLifetime = state.getDefaultLifetime();
		executeWorkflowScript = state.getExecuteWorkflowScript();
		extraArgs = state.getExtraArgs();
		factoryProcessNamePrefix = state.getFactoryProcessNamePrefix();
		javaBinary = state.getJavaBinary();
		maxRuns = state.getMaxRuns();
		serverWorkerJar = state.getServerWorkerJar();
		serverForkerJar = state.getServerForkerJar();
		passwordFile = state.getPasswordFile();
		sleepMS = state.getSleepMS();
		waitSeconds = state.getWaitSeconds();
		registryHost = state.getRegistryHost();
		registryPort = state.getRegistryPort();
		operatingLimit = state.getOperatingLimit();

		loadedState = true;
	}

	@WithinSingleTransaction
	public void store() {
		if (!isPersistent())
			return;
		LocalWorkerModel state = getById(KEY);
		if (state == null) {
			state = persist(makeInstance());
		}

		state.setDefaultLifetime(defaultLifetime);
		state.setExecuteWorkflowScript(executeWorkflowScript);
		state.setExtraArgs(extraArgs);
		state.setFactoryProcessNamePrefix(factoryProcessNamePrefix);
		state.setJavaBinary(javaBinary);
		state.setMaxRuns(maxRuns);
		state.setServerWorkerJar(serverWorkerJar);
		state.setServerForkerJar(serverForkerJar);
		state.setPasswordFile(passwordFile);
		state.setSleepMS(sleepMS);
		state.setWaitSeconds(waitSeconds);
		state.setRegistryHost(registryHost);
		state.setRegistryPort(registryPort);
		state.setOperatingLimit(operatingLimit);

		loadedState = true;
	}
}