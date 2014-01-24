package org.taverna.server.master.worker;

import static org.springframework.jmx.support.MetricType.COUNTER;
import static org.springframework.jmx.support.MetricType.GAUGE;
import static org.taverna.server.master.TavernaServer.JMX_ROOT;

import java.util.List;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.master.factories.ConfigurableRunFactory;
import org.taverna.server.master.localworker.LocalWorkerState;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

@ManagedResource(objectName = JMX_ROOT + "Factory", description = "The factory for runs.")
public abstract class RunFactoryConfiguration implements ConfigurableRunFactory {
	protected Log log = LogFactory.getLog("Taverna.Server.Worker");
	protected LocalWorkerState state;
	protected RunDBSupport runDB;
	private int totalRuns = 0;

	@PreDestroy
	void closeLog() {
		log = null;
	}

	@Autowired(required = true)
	@Order(0)
	void setState(LocalWorkerState state) {
		this.state = state;
	}

	@Autowired(required = true)
	@Order(0)
	void setRunDB(RunDBSupport runDB) {
		this.runDB = runDB;
	}

	/**
	 * Drop any current references to the registry of runs, and kill off that
	 * process.
	 */
	protected abstract void reinitRegistry();

	/**
	 * Drop any current references to the run factory subprocess and kill it
	 * off.
	 */
	protected abstract void reinitFactory();

	/** Count the number of operating runs. */
	protected abstract int operatingCount() throws Exception;

	protected final synchronized void incrementRunCount() {
		totalRuns++;
	}

	@Override
	@ManagedAttribute(description = "Whether it is allowed to start a run executing.", currencyTimeLimit = 30)
	public final boolean isAllowingRunsToStart() {
		try {
			return state.getOperatingLimit() > getOperatingCount();
		} catch (Exception e) {
			log.info("failed to get operating run count", e);
			return false;
		}
	}

	@Override
	@ManagedAttribute(description = "The host holding the RMI registry to communicate via.")
	public final String getRegistryHost() {
		return state.getRegistryHost();
	}

	@Override
	@ManagedAttribute(description = "The host holding the RMI registry to communicate via.")
	public final void setRegistryHost(String host) {
		state.setRegistryHost(host);
		reinitRegistry();
		reinitFactory();
	}

	@Override
	@ManagedAttribute(description = "The port number of the RMI registry. Should not normally be set.")
	public final int getRegistryPort() {
		return state.getRegistryPort();
	}

	@Override
	@ManagedAttribute(description = "The port number of the RMI registry. Should not normally be set.")
	public final void setRegistryPort(int port) {
		state.setRegistryPort(port);
		reinitRegistry();
		reinitFactory();
	}

	@NonNull
	@Override
	@ManagedAttribute(description = "What JAR do we use to start the RMI registry process?")
	public final String getRmiRegistryJar() {
		return state.getRegistryJar();
	}

	@Override
	@ManagedAttribute(description = "What JAR do we use to start the RMI registry process?")
	public final void setRmiRegistryJar(String rmiRegistryJar) {
		state.setRegistryJar(rmiRegistryJar);
		reinitRegistry();
		reinitFactory();
	}

	@Override
	@ManagedAttribute(description = "The maximum number of simultaneous runs supported by the server.", currencyTimeLimit = 300)
	public final int getMaxRuns() {
		return state.getMaxRuns();
	}

	@Override
	@ManagedAttribute(description = "The maximum number of simultaneous runs supported by the server.", currencyTimeLimit = 300)
	public final void setMaxRuns(int maxRuns) {
		state.setMaxRuns(maxRuns);
	}

	/** @return How many minutes should a workflow live by default? */
	@Override
	@ManagedAttribute(description = "How many minutes should a workflow live by default?", currencyTimeLimit = 300)
	public final int getDefaultLifetime() {
		return state.getDefaultLifetime();
	}

	/**
	 * Set how long a workflow should live by default.
	 * 
	 * @param defaultLifetime
	 *            Default lifetime, in minutes.
	 */
	@Override
	@ManagedAttribute(description = "How many minutes should a workflow live by default?", currencyTimeLimit = 300)
	public final void setDefaultLifetime(int defaultLifetime) {
		state.setDefaultLifetime(defaultLifetime);
	}

	/**
	 * @return How many milliseconds to wait between checks to see if a worker
	 *         process has registered.
	 */
	@Override
	@ManagedAttribute(description = "How many milliseconds to wait between checks to see if a worker process has registered.", currencyTimeLimit = 300)
	public final int getSleepTime() {
		return state.getSleepMS();
	}

	/**
	 * @param sleepTime
	 *            How many milliseconds to wait between checks to see if a
	 *            worker process has registered.
	 */
	@Override
	@ManagedAttribute(description = "How many milliseconds to wait between checks to see if a worker process has registered.", currencyTimeLimit = 300)
	public final void setSleepTime(int sleepTime) {
		state.setSleepMS(sleepTime);
	}

	/**
	 * @return How many seconds to wait for a worker process to register itself.
	 */
	@Override
	@ManagedAttribute(description = "How many seconds to wait for a worker process to register itself.", currencyTimeLimit = 300)
	public final int getWaitSeconds() {
		return state.getWaitSeconds();
	}

	/**
	 * @param seconds
	 *            How many seconds to wait for a worker process to register
	 *            itself.
	 */
	@Override
	@ManagedAttribute(description = "How many seconds to wait for a worker process to register itself.", currencyTimeLimit = 300)
	public final void setWaitSeconds(int seconds) {
		state.setWaitSeconds(seconds);
	}

	/** @return The script to run to start running a workflow. */
	@NonNull
	@Override
	@ManagedAttribute(description = "The script to run to start running a workflow.", currencyTimeLimit = 300)
	public final String getExecuteWorkflowScript() {
		return state.getExecuteWorkflowScript();
	}

	/**
	 * @param executeWorkflowScript
	 *            The script to run to start running a workflow.
	 */
	@Override
	@ManagedAttribute(description = "The script to run to start running a workflow.", currencyTimeLimit = 300)
	public final void setExecuteWorkflowScript(String executeWorkflowScript) {
		state.setExecuteWorkflowScript(executeWorkflowScript);
		reinitFactory();
	}

	/** @return The location of the JAR implementing the server worker processes. */
	@NonNull
	@Override
	@ManagedAttribute(description = "The location of the JAR implementing the server worker processes.")
	public final String getServerWorkerJar() {
		return state.getServerWorkerJar();
	}

	/**
	 * @param serverWorkerJar
	 *            The location of the JAR implementing the server worker
	 *            processes.
	 */
	@Override
	@ManagedAttribute(description = "The location of the JAR implementing the server worker processes.")
	public final void setServerWorkerJar(String serverWorkerJar) {
		state.setServerWorkerJar(serverWorkerJar);
		reinitFactory();
	}

	/** @return The list of additional arguments used to make a worker process. */
	@NonNull
	@Override
	@ManagedAttribute(description = "The list of additional arguments used to make a worker process.", currencyTimeLimit = 300)
	public final String[] getExtraArguments() {
		return state.getExtraArgs();
	}

	/**
	 * @param extraArguments
	 *            The list of additional arguments used to make a worker
	 *            process.
	 */
	@Override
	@ManagedAttribute(description = "The list of additional arguments used to make a worker process.", currencyTimeLimit = 300)
	public final void setExtraArguments(@NonNull String[] extraArguments) {
		state.setExtraArgs(extraArguments);
		reinitFactory();
	}

	/** @return Which java executable to run. */
	@NonNull
	@Override
	@ManagedAttribute(description = "Which java executable to run.", currencyTimeLimit = 300)
	public final String getJavaBinary() {
		return state.getJavaBinary();
	}

	/**
	 * @param javaBinary
	 *            Which java executable to run.
	 */
	@Override
	@ManagedAttribute(description = "Which java executable to run.", currencyTimeLimit = 300)
	public final void setJavaBinary(@NonNull String javaBinary) {
		state.setJavaBinary(javaBinary);
		reinitFactory();
	}

	/**
	 * @return A file containing a password to use when running a program as
	 *         another user (e.g., with sudo).
	 */
	@Nullable
	@Override
	@ManagedAttribute(description = "A file containing a password to use when running a program as another user (e.g., with sudo).", currencyTimeLimit = 300)
	public final String getPasswordFile() {
		return state.getPasswordFile();
	}

	/**
	 * @param passwordFile
	 *            A file containing a password to use when running a program as
	 *            another user (e.g., with sudo).
	 */
	@Override
	@ManagedAttribute(description = "A file containing a password to use when running a program as another user (e.g., with sudo).", currencyTimeLimit = 300)
	public final void setPasswordFile(@Nullable String passwordFile) {
		state.setPasswordFile(passwordFile);
		reinitFactory();
	}

	/**
	 * @return The location of the JAR implementing the secure-fork process.
	 */
	@NonNull
	@Override
	@ManagedAttribute(description = "The location of the JAR implementing the secure-fork process.", currencyTimeLimit = 300)
	public final String getServerForkerJar() {
		return state.getServerForkerJar();
	}

	/**
	 * @param serverForkerJar
	 *            The location of the JAR implementing the secure-fork process.
	 */
	@Override
	@ManagedAttribute(description = "The location of the JAR implementing the secure-fork process.", currencyTimeLimit = 300)
	public final void setServerForkerJar(String forkerJarFilename) {
		state.setServerForkerJar(forkerJarFilename);
		reinitFactory();
	}

	/**
	 * @return How many times has a workflow run been spawned by this engine.
	 *         Restarts reset this counter.
	 */
	@Override
	@ManagedMetric(description = "How many times has a workflow run been spawned by this engine.", currencyTimeLimit = 10, metricType = COUNTER, category = "throughput")
	public final synchronized int getTotalRuns() {
		return totalRuns;
	}

	/**
	 * @return How many checks were done for the worker process the last time a
	 *         spawn was tried.
	 */
	@Override
	@ManagedAttribute(description = "How many checks were done for the worker process the last time a spawn was tried.", currencyTimeLimit = 60)
	public abstract int getLastStartupCheckCount();

	@NonNull
	@Override
	@ManagedAttribute(description = "The names of the current runs.", currencyTimeLimit = 5)
	public final String[] getCurrentRunNames() {
		List<String> names = runDB.listRunNames();
		return names.toArray(new String[names.size()]);
	}

	@Override
	@ManagedAttribute(description = "What the factory subprocess's main RMI interface is registered as.", currencyTimeLimit = 60)
	public abstract String getFactoryProcessName();

	/**
	 * @return What was the exit code from the last time the factory subprocess
	 *         was killed?
	 */
	@Override
	@ManagedAttribute(description = "What was the exit code from the last time the factory subprocess was killed?")
	public abstract Integer getLastExitCode();

	/**
	 * @return The mapping of user names to RMI factory IDs.
	 */
	@Override
	@ManagedAttribute(description = "The mapping of user names to RMI factory IDs.", currencyTimeLimit = 60)
	public abstract String[] getFactoryProcessMapping();

	@Override
	@ManagedAttribute(description = "The maximum number of simultaneous operating runs supported by the server.", currencyTimeLimit = 300)
	public final void setOperatingLimit(int operatingLimit) {
		state.setOperatingLimit(operatingLimit);
	}

	@Override
	@ManagedAttribute(description = "The maximum number of simultaneous operating runs supported by the server.", currencyTimeLimit = 300)
	public final int getOperatingLimit() {
		return state.getOperatingLimit();
	}

	/**
	 * @return A count of the number of runs believed to actually be in the
	 *         {@linkplain uk.org.taverna.server.master.common.Status#Operating
	 *         operating} state.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	@Override
	@ManagedMetric(description = "How many workflow runs are currently actually executing.", currencyTimeLimit = 10, metricType = GAUGE, category = "throughput")
	public final int getOperatingCount() throws Exception {
		return operatingCount();
	}

	@Override
	@ManagedAttribute(description="Whether to tell a workflow to generate provenance bundles by default.")
	public final void setGenerateProvenance(boolean genProv) {
		state.setGenerateProvenance(genProv);
	}

	@Override
	@ManagedAttribute(description="Whether to tell a workflow to generate provenance bundles by default.")
	public final boolean getGenerateProvenance() {
		return state.getGenerateProvenance();
	}
}
