package org.taverna.server.master.localworker;

import static org.taverna.server.master.localworker.RunConnections.KEY;
import static org.taverna.server.master.localworker.RunConnections.makeInstance;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Value;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.localworker.PersistentContext.Action;
import org.taverna.server.master.localworker.PersistentContext.Function;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class RunDatabase implements RunStore {
	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	@Required
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		ctx = new PersistentContext<RunConnections>(persistenceManagerFactory);
	}

	@Required
	public void setState(LocalWorkerState state) {
		this.state = state;
	}

	PersistentContext<RunConnections> ctx;
	LocalWorkerState state;

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	private interface Act<Exn extends Exception> {
		public void a(Map<String, RemoteRunDelegate> runs) throws Exn;
	}

	private interface Func<Result, Exn extends Exception> {
		public Result f(Map<String, RemoteRunDelegate> runs) throws Exn;
	}

	private synchronized <Result, Exn extends Exception> Result inTransaction(
			final Func<Result, Exn> fun) throws Exn {
		return ctx.inTransaction(new Function<Result, Exn>() {
			@Override
			public Result act() throws Exn {
				RunConnections c = ctx.getByID(RunConnections.class, KEY);
				if (c == null) {
					c = ctx.persist(makeInstance());
				}
				return fun.f(c.getRuns());
			}
		});
	}

	private synchronized <Exn extends Exception> void inTransaction(
			final Act<Exn> act) throws Exn {
		ctx.inTransaction(new Action<Exn>() {
			@Override
			public void act() throws Exn {
				RunConnections c = ctx.getByID(RunConnections.class, KEY);
				if (c == null) {
					c = ctx.persist(makeInstance());
				}
				act.a(c.getRuns());
			}
		});
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	public int countRuns() {
		return inTransaction(new Func<Integer, RuntimeException>() {
			@Override
			public Integer f(Map<String, RemoteRunDelegate> runs) {
				return runs.size();
			}
		});
	}

	public interface PerRunCallback<Exn extends Exception> {
		public void doit(String name, TavernaRun run) throws Exn;
	}

	public <Exn extends Exception> void iterateOverRuns(
			final PerRunCallback<Exn> cb) throws Exn {
		inTransaction(new Act<Exn>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) throws Exn {
				for (Map.Entry<String, RemoteRunDelegate> e : runs.entrySet()) {
					cb.doit(e.getKey(), e.getValue());
				}
			}
		});
	}

	private TavernaRun get(final String name) {
		return inTransaction(new Func<TavernaRun, RuntimeException>() {
			@Override
			public TavernaRun f(Map<String, RemoteRunDelegate> runs) {
				return runs.get(name);
			}
		});
	}

	/**
	 * How frequently to check for expired workflow runs.
	 */
	public static final int CLEANER_INTERVAL_MS = 30000;
	private Timer timer = new Timer("Taverna.Server.RunDB.Timer", true);
	private TimerTask cleaner;

	public RunDatabase() {
		cleaner = new Cleaner(this);
		timer.scheduleAtFixedRate(cleaner, CLEANER_INTERVAL_MS,
				CLEANER_INTERVAL_MS);
	}

	@Override
	public TavernaRun getRun(Principal user, Policy p, String uuid)
			throws UnknownRunException {
		TavernaRun run = get(uuid);
		if (run != null && (user == null || p.permitAccess(user, run)))
			return run;
		throw new UnknownRunException();
	}

	@Override
	public TavernaRun getRun(String uuid) throws UnknownRunException {
		TavernaRun run = get(uuid);
		if (run != null)
			return run;
		throw new UnknownRunException();
	}

	@Override
	public Map<String, TavernaRun> listRuns(Principal user, Policy p) {
		final Map<String, TavernaRun> result = new HashMap<String, TavernaRun>();
		iterateOverRuns(new PerRunCallback<RuntimeException>() {
			@Override
			public void doit(String name, TavernaRun run) {
				result.put(name, run);
			}
		});
		return result;
	}

	@Override
	public void registerRun(final String name, final TavernaRun run) {
		if (!(run instanceof RemoteRunDelegate))
			throw new IllegalArgumentException(
					"run must be created by localworker package");
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) {
				runs.put(name, (RemoteRunDelegate) run);
			}
		});
	}

	@Override
	public void unregisterRun(final String name) {
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) {
				runs.remove(name);
			}
		});
	}

	void clean(final Date now) {
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) {
				Set<String> toGo = new HashSet<String>();
				for (Map.Entry<String, RemoteRunDelegate> e : runs.entrySet()) {
					if (e.getValue() == null)
						continue;
					if (now.after(e.getValue().getExpiry()))
						toGo.add(e.getKey());
				}
				for (String key : toGo)
					runs.remove(key).destroy();
			}
		});
	}

	public void shutdown() {
		synchronized (this) {
			TimerTask tt = cleaner;
			cleaner = null;
			if (tt != null) {
				tt.cancel();
				timer.cancel();
			}
		}
	}

	@Override
	protected void finalize() {
		shutdown();
	}

	/**
	 * Class that handles cleanup of tasks when their expiry is past.
	 * 
	 * @author Donal Fellows
	 */
	private static class Cleaner extends TimerTask {
		private WeakReference<RunDatabase> arrf;

		Cleaner(RunDatabase arrf) {
			this.arrf = new WeakReference<RunDatabase>(arrf);
		}

		@Override
		public void run() {
			// Reconvert back to a strong reference for the length of this check
			RunDatabase f = arrf.get();
			if (f == null) {
				cancel();
				return;
			}
			// Check to see if anything is needing cleaning; if not, we're done
			f.clean(new Date());
		}
	}

}

/**
 * The class that actually participates in the persistence system.
 * 
 * @author Donal Fellows
 */
@javax.jdo.annotations.PersistenceCapable(table = "RUN_CONNECTIONS")
class RunConnections {
	static RunConnections makeInstance() {
		RunConnections o = new RunConnections();
		o.ID = KEY;
		o.setRuns(new HashMap<String, RemoteRunDelegate>());
		return o;
	}

	@PrimaryKey(column = "ID")
	protected int ID;

	static final int KEY = 1;

	@Persistent
	@Key(unique = "true")
	@Value(serialized = "true")
	@Join(table = "RUN_CONNECTION_MAP")
	private Map<String, RemoteRunDelegate> runs;

	/**
	 * @param runs
	 *            the runs to set
	 */
	public void setRuns(Map<String, RemoteRunDelegate> runs) {
		this.runs = runs;
	}

	/**
	 * @return the runs
	 */
	public Map<String, RemoteRunDelegate> getRuns() {
		return runs == null ? new HashMap<String, RemoteRunDelegate>() : runs;
	}
}
