/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.taverna.server.master.worker.WorkerModel;

/**
 * The actual database connector for persisted local worker state.
 * 
 * @author Donal Fellows
 */
// WARNING! If you change the name of this class, update persistence.xml as
// well!
@PersistenceCapable(table = PersistedState.TABLE)
class PersistedState implements WorkerModel {
	static final String TABLE = "LOCALWORKERSTATE__PERSISTEDSTATE";
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
	private String registryJar;
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
	@Persistent(defaultFetchGroup = "true")
	@Join(table = TABLE + "_PERMWFURI", column = "ID")
	private String[] permittedWorkflows;

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

	@Override
	public List<URI> getPermittedWorkflowURIs() {
		List<URI> uris = new ArrayList<URI>();
		if (permittedWorkflows != null)
			for (String uri : permittedWorkflows)
				try {
					uris.add(new URI(uri));
				} catch (URISyntaxException e) {
					// Ignore; should be impossible at this point
				}
		return uris;
	}

	@Override
	public void setPermittedWorkflowURIs(List<URI> permittedWorkflows) {
		this.permittedWorkflows = new String[permittedWorkflows.size()];
		for (int i = 0 ; i<this.permittedWorkflows.length ; i++)
			this.permittedWorkflows[i] = permittedWorkflows.get(i).toString();
	}

	@Override
	public String getRegistryJar() {
		return registryJar;
	}

	@Override
	public void setRegistryJar(String registryJar) {
		this.registryJar = registryJar;
	}
}