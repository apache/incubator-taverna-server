package org.taverna.server.master.localworker;

import static java.io.File.separator;
import static java.lang.System.getProperty;
import static org.taverna.server.master.localworker.LocalWorkerManagementState.KEY;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Value;

import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * The persistent state of a local worker factory.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class LocalWorkerState {
	/**
	 * The name of the resource that is the implementation of the subprocess
	 * that this class will fork off.
	 */
	public static final String SUBPROCESS_IMPLEMENTATION_JAR = "util/server.worker.jar";

	private interface Action<T extends Throwable> {
		public void act() throws T;
	}

	private interface Function<R, T extends Throwable> {
		public R act() throws T;
	}

	@PersistenceAware
	private class PersistentContext {
		private PersistenceManager pm;

		PersistentContext(PersistenceManagerFactory persistenceManagerFactory) {
			pm = persistenceManagerFactory.getPersistenceManager();
		}

		@SuppressWarnings("unchecked")
		<T> T get(Class<T> cls, int id) {
			Query q = pm.newQuery(RunConnections.class, "id == " + id);
			Collection<T> results = (Collection<T>) q.execute();
			if (!results.isEmpty()) {
				return results.iterator().next();
			}
			return null;
		}

		<T extends Throwable> void xact(Action<T> act) throws T {
			pm.currentTransaction().begin();
			boolean ok = false;
			try {
				act.act();
				pm.currentTransaction().commit();
				ok = true;
			} finally {
				if (!ok)
					pm.currentTransaction().rollback();
			}
		}

		<R, T extends Throwable> R xact(Function<R, T> act) throws T {
			pm.currentTransaction().begin();
			boolean ok = false;
			try {
				R r = act.act();
				pm.currentTransaction().commit();
				ok = true;
				return r;
			} finally {
				if (!ok)
					pm.currentTransaction().rollback();
			}
		}

		<R> R persist(R value) {
			return pm.makePersistent(value);
		}
	}

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		ctx = new PersistentContext(persistenceManagerFactory);
	}

	PersistentContext ctx;

	/** Initial lifetime of runs, in minutes. */
	int defaultLifetime;
	private static final int DEFAULT_DEFAULT_LIFE = 20;
	/**
	 * Maximum number of runs to exist at once. Note that this includes when
	 * they are just existing for the purposes of file transfer (
	 * {@link Status#Initialized}/{@link Status#Finished} states).
	 */
	int maxRuns;
	private static final int DEFAULT_MAX = 5;
	/**
	 * Prefix to use for RMI names.
	 */
	String factoryProcessNamePrefix;
	private static final String DEFAULT_PREFIX = "ForkRunFactory.";
	/**
	 * Full path name of the script used to start running a workflow; normally
	 * expected to be "<i>somewhere/</i><tt>executeWorkflow.sh</tt>".
	 */
	String executeWorkflowScript;
	transient String defaultExecuteWorkflowScript;
	/**
	 * The extra arguments to pass to the subprocess.
	 */
	String[] extraArgs;
	private static final String[] DEFAULT_EXTRA_ARGS = new String[0];
	/**
	 * How long to wait for subprocess startup, in seconds.
	 */
	int waitSeconds;
	private static final int DEFAULT_WAIT = 40;
	/**
	 * Polling interval to use during startup, in milliseconds.
	 */
	int sleepMS;
	private static final int DEFAULT_SLEEP = 1000;
	/**
	 * Full path name to the worker process's implementation JAR.
	 */
	String serverWorkerJar;
	private static final String DEFAULT_WORKER_JAR = LocalWorkerState.class
			.getClassLoader().getResource(SUBPROCESS_IMPLEMENTATION_JAR)
			.getFile();
	/**
	 * Full path name to the Java binary to use to run the subprocess.
	 */
	String javaBinary;
	private static final String DEFAULT_JAVA_BINARY = getProperty("java.home")
			+ separator + "bin" + separator + "java";

	/**
	 * @param defaultLifetime
	 *            how long a workflow run should live by default, in minutes.
	 */
	public void setDefaultLifetime(int defaultLifetime) {
		this.defaultLifetime = defaultLifetime;
		store();
	}

	/**
	 * @return how long a workflow run should live by default, in minutes.
	 */
	public int getDefaultLifetime() {
		load();
		return defaultLifetime < 1 ? DEFAULT_DEFAULT_LIFE : defaultLifetime;
	}

	/**
	 * @param maxRuns
	 *            the maxRuns to set
	 */
	public void setMaxRuns(int maxRuns) {
		this.maxRuns = maxRuns;
		store();
	}

	/**
	 * @return the maxRuns
	 */
	public int getMaxRuns() {
		load();
		return maxRuns < 1 ? DEFAULT_MAX : maxRuns;
	}

	/**
	 * @param factoryProcessNamePrefix
	 *            the factoryProcessNamePrefix to set
	 */
	public void setFactoryProcessNamePrefix(String factoryProcessNamePrefix) {
		this.factoryProcessNamePrefix = factoryProcessNamePrefix;
		store();
	}

	/**
	 * @return the factoryProcessNamePrefix
	 */
	public String getFactoryProcessNamePrefix() {
		load();
		return factoryProcessNamePrefix == null ? DEFAULT_PREFIX
				: factoryProcessNamePrefix;
	}

	/**
	 * @param executeWorkflowScript
	 *            the executeWorkflowScript to set
	 */
	public void setExecuteWorkflowScript(String executeWorkflowScript) {
		this.executeWorkflowScript = executeWorkflowScript;
		store();
	}

	/**
	 * @return the executeWorkflowScript
	 */
	public String getExecuteWorkflowScript() {
		load();
		return executeWorkflowScript == null ? defaultExecuteWorkflowScript
				: executeWorkflowScript;
	}

	/**
	 * @param extraArgs
	 *            the extraArgs to set
	 */
	public void setExtraArgs(String[] extraArgs) {
		this.extraArgs = extraArgs;
		store();
	}

	/**
	 * @return the extraArgs
	 */
	public String[] getExtraArgs() {
		load();
		return extraArgs == null ? DEFAULT_EXTRA_ARGS : extraArgs;
	}

	/**
	 * @param waitSeconds
	 *            the waitSeconds to set
	 */
	public void setWaitSeconds(int waitSeconds) {
		this.waitSeconds = waitSeconds;
		store();
	}

	/**
	 * @return the waitSeconds
	 */
	public int getWaitSeconds() {
		load();
		return waitSeconds < 1 ? DEFAULT_WAIT : waitSeconds;
	}

	/**
	 * @param sleepMS
	 *            the sleepMS to set
	 */
	public void setSleepMS(int sleepMS) {
		this.sleepMS = sleepMS;
		store();
	}

	/**
	 * @return the sleepMS
	 */
	public int getSleepMS() {
		load();
		return sleepMS < 1 ? DEFAULT_SLEEP : sleepMS;
	}

	/**
	 * @param serverWorkerJar
	 *            the serverWorkerJar to set
	 */
	public void setServerWorkerJar(String serverWorkerJar) {
		this.serverWorkerJar = serverWorkerJar;
		store();
	}

	/**
	 * @return the serverWorkerJar
	 */
	public String getServerWorkerJar() {
		load();
		return serverWorkerJar == null ? DEFAULT_WORKER_JAR : serverWorkerJar;
	}

	/**
	 * @param javaBinary
	 *            the javaBinary to set
	 */
	public void setJavaBinary(String javaBinary) {
		this.javaBinary = javaBinary;
		store();
	}

	/**
	 * @return the javaBinary
	 */
	public String getJavaBinary() {
		load();
		return javaBinary == null ? DEFAULT_JAVA_BINARY : javaBinary;
	}

	// --------------------------------------------------------------

	public interface PerRunCallback<T extends Throwable> {
		public void doit(String name, TavernaRun run) throws T;
	}

	public interface RemovalFilter {
		public boolean test(String name, TavernaRun run);
	}

	private interface A<T extends Throwable> {
		public void a(Map<String, TavernaRun> runs) throws T;
	}

	private interface F<R, T extends Throwable> {
		public R f(Map<String, TavernaRun> runs) throws T;
	}

	private <R, T extends Throwable> R xact(final F<R, T> fun) throws T {
		return ctx.xact(new Function<R, T>() {
			@Override
			public R act() throws T {
				RunConnections c = ctx.get(RunConnections.class,
						RunConnections.KEY);
				if (c != null)
					return fun.f(c.getRuns());
				c = new RunConnections();
				c.id = RunConnections.KEY;
				c.setRuns(new HashMap<String, TavernaRun>());
				return fun.f(ctx.persist(c).getRuns());
			}
		});
	}

	private <T extends Throwable> void xact(final A<T> act) throws T {
		ctx.xact(new Action<T>() {
			@Override
			public void act() throws T {
				RunConnections c = ctx.get(RunConnections.class,
						RunConnections.KEY);
				if (c != null) {
					act.a(c.getRuns());
				} else {
					c = new RunConnections();
					c.id = RunConnections.KEY;
					c.setRuns(new HashMap<String, TavernaRun>());
					act.a(ctx.persist(c).getRuns());
				}
			}
		});
	}

	public int countRuns() {
		return xact(new F<Integer, RuntimeException>() {
			@Override
			public Integer f(Map<String, TavernaRun> runs) {
				return runs.size();
			}
		});
	}

	public <T extends Throwable> void iterateOverRuns(final PerRunCallback<T> cb)
			throws T {
		xact(new A<T>() {
			@Override
			public void a(Map<String, TavernaRun> runs) throws T {
				for (Map.Entry<String, TavernaRun> e : runs.entrySet()) {
					cb.doit(e.getKey(), e.getValue());
				}
			}
		});
	}

	public void add(final String name, final TavernaRun run) {
		xact(new A<RuntimeException>() {
			@Override
			public void a(Map<String, TavernaRun> runs) {
				runs.put(name, run);
			}
		});
	}

	public void remove(final String name) {
		xact(new A<RuntimeException>() {
			@Override
			public void a(Map<String, TavernaRun> runs) {
				runs.remove(name);
			}
		});
	}

	public TavernaRun get(final String name) {
		return xact(new F<TavernaRun, RuntimeException>() {
			@Override
			public TavernaRun f(Map<String, TavernaRun> runs) {
				return runs.get(name);
			}
		});
	}

	public void removeWhen(final RemovalFilter filter) {
		xact(new A<RuntimeException>() {
			@Override
			public void a(Map<String, TavernaRun> runs) {
				Set<String> toGo = new HashSet<String>();
				for (Map.Entry<String, TavernaRun> e : runs.entrySet())
					if (filter.test(e.getKey(), e.getValue()))
						toGo.add(e.getKey());
				for (String key : toGo)
					try {
						runs.remove(key).destroy();
					} catch (NoDestroyException e1) {
					}
			}
		});
	}

	// --------------------------------------------------------------

	private boolean loadedState;

	public void load() {
		if (loadedState || ctx == null)
			return;
		ctx.xact(new Action<RuntimeException>() {
			@Override
			public void act() {
				LocalWorkerManagementState state = ctx.get(
						LocalWorkerManagementState.class, KEY);
				if (state != null) {
					defaultLifetime = state.getDefaultLifetime();
					executeWorkflowScript = state.getExecuteWorkflowScript();
					extraArgs = state.getExtraArgs();
					factoryProcessNamePrefix = state
							.getFactoryProcessNamePrefix();
					javaBinary = state.getJavaBinary();
					maxRuns = state.getMaxRuns();
					serverWorkerJar = state.getServerWorkerJar();
					sleepMS = state.getSleepMS();
					waitSeconds = state.getWaitSeconds();
				}
			}
		});
		loadedState = true;
	}

	private void store() {
		if (ctx == null)
			return;
		ctx.xact(new Action<RuntimeException>() {
			@Override
			public void act() {
				LocalWorkerManagementState state = ctx.get(
						LocalWorkerManagementState.class, KEY);
				if (state == null) {
					state = new LocalWorkerManagementState();
					// save state
					state.id = KEY; // whatever...
					state.setDefaultLifetime(defaultLifetime);
					state.setExecuteWorkflowScript(executeWorkflowScript);
					state.setExtraArgs(extraArgs);
					state.setFactoryProcessNamePrefix(factoryProcessNamePrefix);
					state.setJavaBinary(javaBinary);
					state.setMaxRuns(maxRuns);
					state.setServerWorkerJar(serverWorkerJar);
					state.setSleepMS(sleepMS);
					state.setWaitSeconds(waitSeconds);
					state = ctx.persist(state);
				} else {
					state.setDefaultLifetime(defaultLifetime);
					state.setExecuteWorkflowScript(executeWorkflowScript);
					state.setExtraArgs(extraArgs);
					state.setFactoryProcessNamePrefix(factoryProcessNamePrefix);
					state.setJavaBinary(javaBinary);
					state.setMaxRuns(maxRuns);
					state.setServerWorkerJar(serverWorkerJar);
					state.setSleepMS(sleepMS);
					state.setWaitSeconds(waitSeconds);
				}
			}
		});
		loadedState = true;
	}
}

@javax.jdo.annotations.PersistenceCapable
class LocalWorkerManagementState {
	@PrimaryKey
	protected int id;

	public static final int KEY = 32;

	@Persistent
	private int defaultLifetime;
	@Persistent
	private int maxRuns;
	@Persistent
	private String factoryProcessNamePrefix;
	@Persistent
	private String executeWorkflowScript;
	@Persistent
	private String[] extraArgs;
	@Persistent
	private int waitSeconds;
	@Persistent
	private int sleepMS;
	@Persistent
	private String serverWorkerJar;
	@Persistent
	private String javaBinary;

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
}

@PersistenceCapable
class RunConnections {
	@PrimaryKey
	protected int id;

	public static final int KEY = 1;

	@Persistent
	@Key(table = "RUN_CONNECTIONS", unique = "true")
	@Value(serialized = "true")
	private Map<String, TavernaRun> runs;

	/**
	 * @param runs
	 *            the runs to set
	 */
	public void setRuns(Map<String, TavernaRun> runs) {
		this.runs = runs;
	}

	/**
	 * @return the runs
	 */
	public Map<String, TavernaRun> getRuns() {
		return runs == null ? new HashMap<String, TavernaRun>() : runs;
	}
}
