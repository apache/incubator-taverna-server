/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
class LocalWorkerManagementState {
	static LocalWorkerManagementState makeInstance() {
		LocalWorkerManagementState o = new LocalWorkerManagementState();
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

	/**
	 * @param defaultLifetime
	 *            how long a workflow run should live by default, in minutes.
	 */
	public void setDefaultLifetime(int defaultLifetime) {
		this.defaultLifetime = defaultLifetime;
	}

	/**
	 * @return how long a workflow run should live by default, in minutes.
	 */
	public int getDefaultLifetime() {
		return defaultLifetime;
	}

	/**
	 * @param maxRuns
	 *            the maxRuns to set
	 */
	public void setMaxRuns(int maxRuns) {
		this.maxRuns = maxRuns;
	}

	/**
	 * @return the maxRuns
	 */
	public int getMaxRuns() {
		return maxRuns;
	}

	/**
	 * @param factoryProcessNamePrefix
	 *            the factoryProcessNamePrefix to set
	 */
	public void setFactoryProcessNamePrefix(String factoryProcessNamePrefix) {
		this.factoryProcessNamePrefix = factoryProcessNamePrefix;
	}

	/**
	 * @return the factoryProcessNamePrefix
	 */
	public String getFactoryProcessNamePrefix() {
		return factoryProcessNamePrefix;
	}

	/**
	 * @param executeWorkflowScript
	 *            the executeWorkflowScript to set
	 */
	public void setExecuteWorkflowScript(String executeWorkflowScript) {
		this.executeWorkflowScript = executeWorkflowScript;
	}

	/**
	 * @return the executeWorkflowScript
	 */
	public String getExecuteWorkflowScript() {
		return executeWorkflowScript;
	}

	/**
	 * @param extraArgs
	 *            the extraArgs to set
	 */
	public void setExtraArgs(String[] extraArgs) {
		this.extraArgs = extraArgs;
	}

	/**
	 * @return the extraArgs
	 */
	public String[] getExtraArgs() {
		return extraArgs;
	}

	/**
	 * @param waitSeconds
	 *            the waitSeconds to set
	 */
	public void setWaitSeconds(int waitSeconds) {
		this.waitSeconds = waitSeconds;
	}

	/**
	 * @return the waitSeconds
	 */
	public int getWaitSeconds() {
		return waitSeconds;
	}

	/**
	 * @param sleepMS
	 *            the sleepMS to set
	 */
	public void setSleepMS(int sleepMS) {
		this.sleepMS = sleepMS;
	}

	/**
	 * @return the sleepMS
	 */
	public int getSleepMS() {
		return sleepMS;
	}

	/**
	 * @param serverWorkerJar
	 *            the serverWorkerJar to set
	 */
	public void setServerWorkerJar(String serverWorkerJar) {
		this.serverWorkerJar = serverWorkerJar;
	}

	/**
	 * @return the serverWorkerJar
	 */
	public String getServerWorkerJar() {
		return serverWorkerJar;
	}

	/**
	 * @param javaBinary
	 *            the javaBinary to set
	 */
	public void setJavaBinary(String javaBinary) {
		this.javaBinary = javaBinary;
	}

	/**
	 * @return the javaBinary
	 */
	public String getJavaBinary() {
		return javaBinary;
	}

	/**
	 * @param registryPort
	 *            the registryPort to set
	 */
	public void setRegistryPort(int registryPort) {
		this.registryPort = registryPort;
	}

	/**
	 * @return the registryPort
	 */
	public int getRegistryPort() {
		return registryPort;
	}

	/**
	 * @param registryHost
	 *            the registryHost to set
	 */
	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	/**
	 * @return the registryHost
	 */
	public String getRegistryHost() {
		return registryHost;
	}

	/**
	 * @param serverForkerJar
	 *            the serverForkerJar to set
	 */
	public void setServerForkerJar(String serverForkerJar) {
		this.serverForkerJar = serverForkerJar;
	}

	/**
	 * @return the serverForkerJar
	 */
	public String getServerForkerJar() {
		return serverForkerJar;
	}

	/**
	 * @param passwordFile
	 *            the passwordFile to set
	 */
	public void setPasswordFile(String passwordFile) {
		this.passwordFile = passwordFile;
	}

	/**
	 * @return the passwordFile
	 */
	public String getPasswordFile() {
		return passwordFile;
	}
}