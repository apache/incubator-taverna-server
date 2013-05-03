/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * The actual database connector for persisted local worker state.
 * 
 * @author Donal Fellows
 */
// WARNING! If you change the name of this class, update persistence.xml as
// well!
@PersistenceCapable(table = "LOCALWORKERSTATE__PERSISTEDSTATE")
class PersistedState implements LocalWorkerModel {
	static PersistedState makeInstance() {
		PersistedState o = new PersistedState();
		o.ID = KEY;
		return o;
	}

	@PrimaryKey(column = "ID")
	protected int ID;

	static final int KEY = 32;

	@Persistent
	private int defaultLifetime;
	@Persistent
	private int maxRuns;
	@Persistent
	private String factoryProcessNamePrefix;
	@Persistent
	private String executeWorkflowScript;
	@Persistent(serialized = "true")
	private String[] extraArgs;
	@Persistent
	private int waitSeconds;
	@Persistent
	private int sleepMS;
	@Persistent
	private String serverWorkerJar;
	@Persistent
	private String serverForkerJar;
	@Persistent
	private String passwordFile;
	@Persistent
	private String javaBinary;
	@Persistent
	private int registryPort;
	@Persistent
	private String registryHost;
	@Persistent
	private int operatingLimit;

	@Override
	public void setDefaultLifetime(int defaultLifetime) {
		this.defaultLifetime = defaultLifetime;
	}

	@Override
	public int getDefaultLifetime() {
		return defaultLifetime;
	}

	@Override
	public void setMaxRuns(int maxRuns) {
		this.maxRuns = maxRuns;
	}

	@Override
	public int getMaxRuns() {
		return maxRuns;
	}

	@Override
	public void setFactoryProcessNamePrefix(String factoryProcessNamePrefix) {
		this.factoryProcessNamePrefix = factoryProcessNamePrefix;
	}

	@Override
	public String getFactoryProcessNamePrefix() {
		return factoryProcessNamePrefix;
	}

	@Override
	public void setExecuteWorkflowScript(String executeWorkflowScript) {
		this.executeWorkflowScript = executeWorkflowScript;
	}

	@Override
	public String getExecuteWorkflowScript() {
		return executeWorkflowScript;
	}

	@Override
	public void setExtraArgs(String[] extraArgs) {
		this.extraArgs = extraArgs;
	}

	@Override
	public String[] getExtraArgs() {
		return extraArgs;
	}

	@Override
	public void setWaitSeconds(int waitSeconds) {
		this.waitSeconds = waitSeconds;
	}

	@Override
	public int getWaitSeconds() {
		return waitSeconds;
	}

	@Override
	public void setSleepMS(int sleepMS) {
		this.sleepMS = sleepMS;
	}

	@Override
	public int getSleepMS() {
		return sleepMS;
	}

	@Override
	public void setServerWorkerJar(String serverWorkerJar) {
		this.serverWorkerJar = serverWorkerJar;
	}

	@Override
	public String getServerWorkerJar() {
		return serverWorkerJar;
	}

	@Override
	public void setJavaBinary(String javaBinary) {
		this.javaBinary = javaBinary;
	}

	@Override
	public String getJavaBinary() {
		return javaBinary;
	}

	@Override
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	@Override
	public int getRegistryPort() {
		return registryPort;
	}

	@Override
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	@Override
	public String getRegistryHost() {
		return registryHost;
	}

	@Override
	public void setServerForkerJar(String serverForkerJar) {
		this.serverForkerJar = serverForkerJar;
	}

	@Override
	public String getServerForkerJar() {
		return serverForkerJar;
	}

	@Override
	public void setPasswordFile(String passwordFile) {
		this.passwordFile = passwordFile;
	}

	@Override
	public String getPasswordFile() {
		return passwordFile;
	}

	@Override
	public void setOperatingLimit(int operatingLimit) {
		this.operatingLimit = operatingLimit;
	}

	@Override
	public int getOperatingLimit() {
		return operatingLimit;
	}
}