/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.localworker.RunConnection.toDBform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.annotations.PersistenceAware;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.notification.NotificationEngine;
import org.taverna.server.master.utils.UsernamePrincipal;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class RunDatabaseDAO implements RunStore, RunDBSupport {
	private Log log = LogFactory.getLog("Taverna.Server.LocalWorker.RunDB");
	private List<CompletionNotifier> notifier = new ArrayList<CompletionNotifier>();
	private NotificationEngine notificationEngine;
	private PersistenceManager pm;

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	@Required
	@Override
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		pm = persistenceManagerFactory.getPersistenceManagerProxy();
	}

	@Override
	public void setNotifier(CompletionNotifier n) {
		notifier = asList(n);
	}

	@Required
	@Override
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	private Transaction init() {
		Transaction tx = pm.currentTransaction();
		if (tx.isActive())
			return null;
		tx.begin();
		return tx;
	}

	private void done(Transaction tx) {
		if (tx != null)
			tx.commit();
		pm.close();
	}

	private void rollback(Transaction tx) {
		if (tx != null)
			tx.rollback();
		pm.close();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	@SuppressWarnings("unchecked")
	private List<String> nameRuns() {
		log.debug("fetching all run names");
		return (List<String>) pm.newNamedQuery(RunConnection.class, "names")
				.execute();
	}

	private Integer count() {
		log.debug("counting the number of runs");
		return (Integer) pm.newNamedQuery(RunConnection.class, "count")
				.execute();
	}

	@SuppressWarnings("unchecked")
	private List<String> expiredRuns() {
		return (List<String>) pm.newNamedQuery(RunConnection.class, "timedout")
				.execute();
	}

	private RunConnection pickRun(String name) {
		log.debug("fetching the run called " + name);
		try {
			RunConnection rc = pm.getObjectById(RunConnection.class, name);
			if (rc == null)
				log.warn("no result for " + name);
			return rc;
		} catch (RuntimeException e) {
			log.warn("problem in fetch", e);
			throw e;
		}
	}

	private void persist(RemoteRunDelegate rrd) throws IOException {
		pm.makePersistent(toDBform(rrd));
	}

	private List<RunConnection> allRuns() {
		try {
			List<RunConnection> rcs = new ArrayList<RunConnection>();
			List<String> names = nameRuns();
			for (String id : names) {
				try {
					if (id != null)
						rcs.add(pickRun(id));
				} catch (RuntimeException e) {
					continue;
				}
			}
			return rcs;
		} catch (RuntimeException e) {
			log.warn("problem in fetch", e);
			throw e;
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	@Override
	public int countRuns() {
		Transaction tx = init();
		try {
			return count();
		} finally {
			done(tx);
		}
	}

	private TavernaRun get(String name) {
		Transaction tx = init();
		try {
			RunConnection rc = pickRun(name);
			return (rc == null) ? null : rc.fromDBform(this);
		} catch (Exception e) {
			return null;
		} finally {
			done(tx);
		}
	}

	@Override
	public TavernaRun getRun(UsernamePrincipal user, Policy p, String uuid)
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
	public Map<String, TavernaRun> listRuns(UsernamePrincipal user, Policy p) {
		Transaction tx = init();
		try {
			Map<String, TavernaRun> result = new HashMap<String, TavernaRun>();
			for (String id : nameRuns()) {
				try {
					RemoteRunDelegate rrd = pickRun(id).fromDBform(this);
					if (p.permitAccess(user, rrd))
						result.put(id, rrd);
				} catch (Exception e) {
					continue;
				}
			}
			return result;
		} finally {
			done(tx);
		}
	}

	@Override
	public List<String> listRunNames() {
		Transaction tx = init();
		try {
			ArrayList<String> runNames = new ArrayList<String>();
			for (RunConnection rc : allRuns()) {
				if (rc.getId() != null)
					runNames.add(rc.getId());
			}
			return runNames;
		} finally {
			done(tx);
		}
	}

	@Override
	public RemoteRunDelegate pickArbitraryRun() throws Exception {
		Transaction tx = init();
		try {
			for (RunConnection rc : allRuns()) {
				if (rc.getId() == null)
					continue;
				return rc.fromDBform(this);
			}
			return null;
		} finally {
			done(tx);
		}
	}

	private void logLength(String message, Object obj) {
		if (!log.isDebugEnabled())
			return;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
			log.debug(message + ": " + baos.size());
		} catch (Exception e) {
			log.warn("oops", e);
		}
	}

	@Override
	public String registerRun(TavernaRun run) {
		if (!(run instanceof RemoteRunDelegate))
			throw new IllegalArgumentException(
					"run must be created by localworker package");
		RemoteRunDelegate rrd = (RemoteRunDelegate) run;
		if (rrd.id == null)
			rrd.id = randomUUID().toString();
		logLength("RemoteRunDelegate serialized length", rrd);
		Transaction tx = init();
		try {
			persist(rrd);
			done(tx);
		} catch (IOException e) {
			rollback(tx);
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
		return rrd.getId();
	}

	@Override
	public void unregisterRun(String name) {
		Transaction tx = init();
		try {
			RunConnection rc = pickRun(name);
			if (rc != null)
				pm.deletePersistent(rc);
			done(tx);
		} catch (RuntimeException e) {
			log.debug("problem persisting the deletion of the run " + name, e);
			rollback(tx);
		}
	}

	@Override
	public void flushToDisk(RemoteRunDelegate run) {
		Transaction tx = init();
		try {
			RunConnection rc = pm.getObjectById(RunConnection.class, run.id);
			rc.makeChanges(run);
			done(tx);
		} catch (IOException e) {
			rollback(tx);
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
	}

	@Override
	public void cleanNow() {
		Transaction tx = init();
		try {
			log.debug("deleting runs that timed out before " + new Date());
			List<String> toDelete = expiredRuns();
			log.debug("found " + toDelete.size() + " runs to delete");
			for (String id : toDelete)
				pm.deletePersistent(pm.getObjectById(RunConnection.class, id));
			done(tx);
		} catch (Exception e) {
			log.warn("failure during deletion of expired runs", e);
			rollback(tx);
		}
	}

	@Override
	public void checkForFinishNow() {
		List<RemoteRunDelegate> toNotify = new ArrayList<RemoteRunDelegate>();
		Transaction tx = init();
		try {
			if (count() == 0)
				return;
			for (RunConnection rc : allRuns()) {
				try {
					RemoteRunDelegate rrd = rc.fromDBform(this);
					if (rrd.doneTransitionToFinished
							|| rrd.getStatus() != Status.Finished)
						continue;
					rrd.doneTransitionToFinished = true;
					rc.setFinished(true);
					toNotify.add(rrd);
				} catch (Exception e) {
					log.warn("failed to do notification of completion", e);
					continue;
				}
			}
		} finally {
			done(tx);
		}
		// Do this _outside_ the context of the transaction!
		for (RemoteRunDelegate rrd : toNotify)
			for (Listener l : rrd.getListeners())
				if (l.getName().equals("io")) {
					try {
						notifyFinished(rrd.id, l, rrd);
					} catch (Exception e) {
						log.warn("failed to do notification of completion", e);
					}
					break;
				}
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
	private void notifyFinished(String name, Listener io, RemoteRunDelegate run)
			throws Exception {
		if (notifier == null)
			return;
		String to = io.getProperty("notificationAddress");
		int code;
		try {
			code = parseInt(io.getProperty("exitcode"));
		} catch (NumberFormatException nfe) {
			// Ignore; not much we can do here...
			return;
		}

		for (CompletionNotifier n : notifier)
			notificationEngine.dispatchMessage(run, to,
					n.makeMessageSubject(name, run, code),
					n.makeCompletionMessage(name, run, code));
	}
}

interface RunDBSupport {
	/**
	 * Scan each run to see if it has finished yet and issue registered
	 * notifications if it has.
	 */
	void checkForFinishNow();

	/**
	 * Remove currently-expired runs from this database.
	 */
	void cleanNow();

	/** How many runs are stored in the database. */
	int countRuns();

	/**
	 * Ensure that a run gets persisted in the database. It is assumed that the
	 * value is already in there.
	 * 
	 * @param run
	 *            The run to persist.
	 */
	void flushToDisk(@NonNull RemoteRunDelegate run);

	/** Select an arbitrary representative run. */
	@Nullable
	RemoteRunDelegate pickArbitraryRun() throws Exception;

	/** Get a list of all the run names. */
	@NonNull
	List<String> listRunNames();

	void setNotificationEngine(NotificationEngine notificationEngine);

	void setNotifier(CompletionNotifier notifier);

	void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory);
}
