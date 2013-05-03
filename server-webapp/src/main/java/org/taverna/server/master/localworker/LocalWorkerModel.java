/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.localworker;

import org.taverna.server.master.common.Status;
import org.taverna.server.master.worker.PolicyLimits;

/**
 * Profile of the getters and setters in a worker system. Ensures that the
 * persisted state matches the public view on the state model at least fairly
 * closely.
 * 
 * @author Donal Fellows
 */
public interface LocalWorkerModel extends PolicyLimits {

	/**
	 * @param defaultLifetime
	 *            how long a workflow run should live by default, in minutes.
	 */
	public abstract void setDefaultLifetime(int defaultLifetime);

	/**
	 * @return how long a workflow run should live by default, in minutes.
	 */
	public abstract int getDefaultLifetime();

	/**
	 * @param maxRuns
	 *            the maximum number of extant workflow runs
	 */
	public abstract void setMaxRuns(int maxRuns);

	/**
	 * @param factoryProcessNamePrefix
	 *            the prefix used for factory processes in RMI
	 */
	public abstract void setFactoryProcessNamePrefix(
			String factoryProcessNamePrefix);

	/**
	 * @return the prefix used for factory processes in RMI
	 */
	public abstract String getFactoryProcessNamePrefix();

	/**
	 * @param executeWorkflowScript
	 *            the script to run to actually run a workflow
	 */
	public abstract void setExecuteWorkflowScript(String executeWorkflowScript);

	/**
	 * @return the script to run to actually run a workflow
	 */
	public abstract String getExecuteWorkflowScript();

	/**
	 * @param extraArgs
	 *            the extra arguments to pass into the workflow runner
	 */
	public abstract void setExtraArgs(String[] extraArgs);

	/**
	 * @return the extra arguments to pass into the workflow runner
	 */
	public abstract String[] getExtraArgs();

	/**
	 * @param waitSeconds
	 *            the number of seconds to wait for subprocesses to start
	 */
	public abstract void setWaitSeconds(int waitSeconds);

	/**
	 * @return the number of seconds to wait for subprocesses to start
	 */
	public abstract int getWaitSeconds();

	/**
	 * @param sleepMS
	 *            milliseconds to wait between polling for a started
	 *            subprocess's status
	 */
	public abstract void setSleepMS(int sleepMS);

	/**
	 * @return milliseconds to wait between polling for a started subprocess's
	 *         status
	 */
	public abstract int getSleepMS();

	/**
	 * @param serverWorkerJar
	 *            the full path name of the file system access worker
	 *            subprocess's implementation JAR
	 */
	public abstract void setServerWorkerJar(String serverWorkerJar);

	/**
	 * @return the full path name of the file system access worker subprocess's
	 *         implementation JAR
	 */
	public abstract String getServerWorkerJar();

	/**
	 * @param javaBinary
	 *            the full path name to the Java binary to use
	 */
	public abstract void setJavaBinary(String javaBinary);

	/**
	 * @return the full path name to the Java binary to use
	 */
	public abstract String getJavaBinary();

	/**
	 * @param registryPort
	 *            what port is the RMI registry on
	 */
	public abstract void setRegistryPort(int registryPort);

	/**
	 * @return what port is the RMI registry on
	 */
	public abstract int getRegistryPort();

	/**
	 * @param registryHost
	 *            what host (network interface) is the RMI registry on
	 */
	public abstract void setRegistryHost(String registryHost);

	/**
	 * @return what host (network interface) is the RMI registry on
	 */
	public abstract String getRegistryHost();

	/**
	 * @param serverForkerJar
	 *            the full path name of the impersonation engine's
	 *            implementation JAR
	 */
	public abstract void setServerForkerJar(String serverForkerJar);

	/**
	 * @return the full path name of the impersonation engine's implementation
	 *         JAR
	 */
	public abstract String getServerForkerJar();

	/**
	 * @param passwordFile
	 *            the full path name of a file containing a password to use with
	 *            sudo (or empty for none)
	 */
	public abstract void setPasswordFile(String passwordFile);

	/**
	 * @return the full path name of a file containing a password to use with
	 *         sudo (or empty for none)
	 */
	public abstract String getPasswordFile();

	/**
	 * @param operatingLimit
	 *            the maximum number of runs in the
	 *            {@linkplain Status#Operating operating} state at once
	 */
	public abstract void setOperatingLimit(int operatingLimit);
}