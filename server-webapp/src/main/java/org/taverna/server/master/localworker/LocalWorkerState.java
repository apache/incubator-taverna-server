package org.taverna.server.master.localworker;

import static java.io.File.separator;
import static java.lang.System.getProperty;

import java.util.HashMap;
import java.util.Map;

import org.taverna.server.master.interfaces.TavernaRun;

/**
 * The persistent state of a local worker factory.
 * 
 * @author Donal Fellows
 */
public class LocalWorkerState {
	/** Initial lifetime of runs, in minutes. */
	private int defaultLifetime = 20;
	private int maxRuns = 5;
	private String factoryProcessNamePrefix = "ForkRunFactory.";
	private String executeWorkflowScript;
	/**
	 * The extra arguments to pass to the subprocess.
	 */
	private String[] extraArgs;
	private int waitSeconds = 40;
	private int sleepMS = 1000;
	private String serverWorkerJar;

	public Map<String, TavernaRun> runs = new HashMap<String, TavernaRun>();
	private String javaBinary = getProperty("java.home") + separator + "bin"
			+ separator + "java";

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
	 * @param maxRuns the maxRuns to set
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
	 * @param factoryProcessNamePrefix the factoryProcessNamePrefix to set
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
	 * @param executeWorkflowScript the executeWorkflowScript to set
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
	 * @param extraArgs the extraArgs to set
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
	 * @param waitSeconds the waitSeconds to set
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
	 * @param sleepMS the sleepMS to set
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
	 * @param serverWorkerJar the serverWorkerJar to set
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
	 * @param javaBinary the javaBinary to set
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
}
