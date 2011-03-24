/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static org.taverna.server.master.localworker.RunConnections.KEY;
import static org.taverna.server.master.localworker.RunConnections.makeInstance;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Value;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.localworker.PersistentContext.Action;
import org.taverna.server.master.localworker.PersistentContext.Function;
import org.taverna.server.master.notification.NotificationEngine;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
@SuppressWarnings("IS2_INCONSISTENT_SYNC")
public class RunDatabase implements RunStore {
	Log log = LogFactory.getLog("Taverna.Server.LocalWorker");

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	@Required
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		ctx = new PersistentContext<RunConnections>(persistenceManagerFactory);
	}

	public void setNotifier(CompletionNotifier n) {
		notifier = asList(n);
	}

	@Required
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	PersistentContext<RunConnections> ctx;
	List<CompletionNotifier> notifier = new ArrayList<CompletionNotifier>();
	private NotificationEngine notificationEngine;

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
	public String registerRun(TavernaRun run) {
		if (!(run instanceof RemoteRunDelegate))
			throw new IllegalArgumentException(
					"run must be created by localworker package");
		final RemoteRunDelegate rrd = (RemoteRunDelegate) run;
		if (rrd.id == null)
			rrd.id = UUID.randomUUID().toString();
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) {
				runs.put(rrd.getId(), rrd);
			}
		});
		return rrd.getId();
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

	void flushToDisk(final RemoteRunDelegate run) {
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs)
					throws RuntimeException {
				for (Map.Entry<String, RemoteRunDelegate> entry : runs
						.entrySet())
					if (entry.getValue().equals(run)) {
						runs.put(entry.getKey(), run);
						return;
					}
			}
		});
	}

	/**
	 * Remove currently-expired runs from this database.
	 */
	public void cleanNow() {
		final Date now = new Date();
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

	/**
	 * Scan each run to see if it has finished yet and issue registered
	 * notifications if it has.
	 */
	public void checkForFinishNow() {
		inTransaction(new Act<RuntimeException>() {
			@Override
			public void a(Map<String, RemoteRunDelegate> runs) {
				for (Map.Entry<String, RemoteRunDelegate> run : runs.entrySet())
					checkForFinishedAndNotify(run.getKey(), run.getValue());
			}

			// Factored out for more mnemonic-ness.
			private void checkForFinishedAndNotify(String name,
					RemoteRunDelegate run) {
				if (run.doneTransitionToFinished
						|| run.getStatus() != Status.Finished)
					return;
				run.doneTransitionToFinished = true;
				try {
					for (Listener l : run.getListeners())
						if (l.getName().equals("io")) {
							notifyFinished(name, l, run);
							return;
						}
				} catch (Exception e) {
					log.warn("failed to do notification of completion", e);
				}
			}
		});
	}

	/**
	 * Process the event that a run has finished.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param io
	 *            The io listener of the run (used to get information about the
	 *            run).
	 * @param run
	 *            The handle to the run.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	void notifyFinished(String name, Listener io, RemoteRunDelegate run)
			throws Exception {
		if (notifier == null)
			return;
		String to = io.getProperty("notificationAddress");
		if (to == null || to.trim().isEmpty())
			return;
		int code;
		try {
			code = parseInt(io.getProperty("exitcode"));
		} catch (NumberFormatException nfe) {
			// Ignore; not much we can do here...
			return;
		}

		for (CompletionNotifier n : notifier)
			notificationEngine.dispatchMessage(to,
					n.makeMessageSubject(name, run, code),
					n.makeCompletionMessage(name, run, code));
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
