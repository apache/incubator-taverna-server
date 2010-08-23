package org.taverna.server.master.localworker;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.localworker.LocalWorkerState.PerRunCallback;
import org.taverna.server.master.localworker.LocalWorkerState.RemovalFilter;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
public class RunDatabase implements RunStore {
	/**
	 * How frequently to check for expired workflow runs.
	 */
	public static final int CLEANER_INTERVAL_MS = 30000;
	private Timer timer = new Timer("Taverna.Server.RunDB.Timer", true);
	private TimerTask cleaner;

	public RunDatabase() {
		cleaner = new RunDBCleaner(this);
		timer.scheduleAtFixedRate(cleaner, CLEANER_INTERVAL_MS,
				CLEANER_INTERVAL_MS);
	}

	LocalWorkerState state;

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(LocalWorkerState state) {
		this.state = state;
	}

	@Override
	public synchronized TavernaRun getRun(Principal user, Policy p, String uuid)
			throws UnknownRunException {
		TavernaRun run = state.get(uuid);
		if (run != null)
			return run;
		throw new UnknownRunException();
	}

	@Override
	public synchronized TavernaRun getRun(String uuid)
			throws UnknownRunException {
		TavernaRun run = state.get(uuid);
		if (run != null)
			return run;
		throw new UnknownRunException();
	}

	@Override
	public synchronized Map<String, TavernaRun> listRuns(Principal user,
			Policy p) {
		final Map<String, TavernaRun> result = new HashMap<String, TavernaRun>();
		state.iterateOverRuns(new PerRunCallback<RuntimeException>() {
			@Override
			public void doit(String name, TavernaRun run) {
				result.put(name, run);
			}
		});
		return result;
	}

	@Override
	public synchronized void registerRun(final String uuid, TavernaRun run) {
		state.add(uuid, run);
	}

	@Override
	public synchronized void unregisterRun(String uuid) {
		state.remove(uuid);
	}

	@Override
	protected synchronized void finalize() {
		cleaner.cancel();
		state.iterateOverRuns(new PerRunCallback<RuntimeException>() {
			@Override
			public void doit(String name, TavernaRun run) {
				try {
					run.destroy();
				} catch (NoDestroyException e) {
				}
			}
		});
		timer.cancel();
	}
}

/**
 * Class that handles cleanup of tasks when their expiry is past.
 * 
 * @author Donal Fellows
 */
class RunDBCleaner extends TimerTask {
	private WeakReference<RunDatabase> arrf;

	RunDBCleaner(RunDatabase arrf) {
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
		final Date now = new Date();
		synchronized (f) {
			f.state.removeWhen(new RemovalFilter() {
				@Override
				public boolean test(String name, TavernaRun run) {
					if (run == null)
						return false;
					return now.after(run.getExpiry());
				}
			});
		}
	}
}
