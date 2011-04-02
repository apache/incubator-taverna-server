/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class RunDatabase implements RunStore {
	Log log = LogFactory.getLog("Taverna.Server.LocalWorker.RunDB");

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	@Required
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		pm = persistenceManagerFactory.getPersistenceManagerProxy();
	}

	public void setNotifier(CompletionNotifier n) {
		notifier = asList(n);
	}

	@Required
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	private PersistenceManager pm;
	List<CompletionNotifier> notifier = new ArrayList<CompletionNotifier>();
	private NotificationEngine notificationEngine;

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	private Transaction initTx(PersistenceManager pm) {
		Transaction tx = pm.currentTransaction();
		if (tx.isActive())
			return null;
		tx.begin();
		return tx;
	}

	private static Transaction commitTx(Transaction tx) {
		if (tx != null)
			tx.commit();
		return null;
	}

	private static Transaction rollbackTx(Transaction tx) {
		if (tx != null)
			tx.rollback();
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<RunConnection> allRuns(PersistenceManager pm) {
		log.debug("fetching all runs");
		try {
			List<RunConnection> rcs = new ArrayList<RunConnection>();
			List<String> names = (List<String>) pm.newNamedQuery(
					RunConnection.class, "names").execute();
			for (String id : names) {
				if (id == null)
					continue;
				RunConnection rc = pm.getObjectById(RunConnection.class, id);
				if (rc == null) {
					log.warn("problem in fetch of " + id);
					continue;
				}
				rcs.add(rc);
			}
			return rcs;
		} catch (RuntimeException e) {
			log.warn("problem in fetch", e);
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	private List<RunConnection> timedoutRuns(PersistenceManager pm, Date now) {
		log.debug("fetching runs that timed out before " + now);
		return (List<RunConnection>) pm.newNamedQuery(RunConnection.class,
				"timedout").execute(now);
	}

	private RunConnection pickRun(PersistenceManager pm, String name) {
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

	private Integer count(PersistenceManager pm) {
		log.debug("counting the number of runs");
		return (Integer) pm.newNamedQuery(RunConnection.class, "count")
				.execute();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	public int countRuns() {
		Transaction tx = initTx(pm);
		try {
			return count(pm);
		} finally {
			commitTx(tx);
			pm.close();
		}
	}

	private TavernaRun get(String name) {
		Transaction tx = initTx(pm);
		try {
			RunConnection rc = pickRun(pm, name);
			return (rc == null) ? null : rc.fromDBform();
		} catch (Exception e) {
			return null;
		} finally {
			commitTx(tx);
			pm.close();
		}
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
		Transaction tx = initTx(pm);
		try {
			Map<String, TavernaRun> result = new HashMap<String, TavernaRun>();
			for (RunConnection rc : allRuns(pm)) {
				try {
					if (rc.getId() != null) {
						RemoteRunDelegate rrd = rc.fromDBform();
						if (p.permitAccess(user, rrd))
							result.put(rc.getId(), rrd);
					}
				} catch (Exception e) {
					continue;
				}
			}
			tx = commitTx(tx);
			return result;
		} finally {
			rollbackTx(tx);
			pm.close();
		}
	}

	List<String> listRunNames() {
		Transaction tx = initTx(pm);
		try {
			ArrayList<String> runNames = new ArrayList<String>();
			// @SuppressWarnings("unchecked")
			// Set<RunConnection> s = (Set<RunConnection>) pm
			// .getManagedObjects(RunConnection.class);
			for (RunConnection rc : allRuns(pm)) {
				if (rc.getId() != null)
					runNames.add(rc.getId());
			}
			return runNames;
		} finally {
			commitTx(tx);
		}
	}

	List<String> listListenerTypes() throws RemoteException, Exception {
		Transaction tx = initTx(pm);
		try {
			// @SuppressWarnings("unchecked")
			// Set<RunConnection> s = (Set<RunConnection>) pm
			// .getManagedObjects(RunConnection.class);
			for (RunConnection rc : allRuns(pm)) {
				if (rc.getId() == null)
					continue;
				return rc.fromDBform().run.getListenerTypes();
			}
			return new ArrayList<String>();
		} finally {
			commitTx(tx);
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
			rrd.id = UUID.randomUUID().toString();
		logLength("RemoteRunDelegate serialized length", rrd);
		Transaction tx = initTx(pm);
		try {
			pm.makePersistent(RunConnection.toDBform(rrd));
		} catch (Exception e) {
			tx = rollbackTx(tx);
			throw new RuntimeException(
					"unexpected problem storing run record in database", e);
		} finally {
			commitTx(tx);
			pm.close();
		}
		return rrd.getId();
	}

	@Override
	public void unregisterRun(String name) {
		Transaction tx = initTx(pm);
		try {
			RunConnection rc = pickRun(pm, name);
			if (rc != null)
				pm.deletePersistent(rc);
		} finally {
			commitTx(tx);
			pm.close();
		}
	}

	void flushToDisk(RemoteRunDelegate run) {
		Transaction tx = initTx(pm);
		try {
			pm.makePersistent(RunConnection.toDBform(run));
		} catch (IOException e) {
			tx = rollbackTx(tx);
			throw new RuntimeException(
					"unexpected problem when persisting updated run record", e);
		} finally {
			commitTx(tx);
			pm.close();
		}
	}

	/**
	 * Remove currently-expired runs from this database.
	 */
	public void cleanNow() {
		final Date now = new Date();
		Transaction tx = initTx(pm);
		try {
			for (RunConnection rc : timedoutRuns(pm, now))
				pm.deletePersistent(rc);
		} catch (Exception e) {
			tx = rollbackTx(tx);
		} finally {
			commitTx(tx);
			pm.close();
		}
	}

	/**
	 * Scan each run to see if it has finished yet and issue registered
	 * notifications if it has.
	 */
	public void checkForFinishNow() {
		Transaction tx = initTx(pm);
		try {
			if (count(pm) == 0)
				return;
			for (RunConnection rc : allRuns(pm)) {
				try {
					checkOneForFinish(rc, rc.fromDBform());
				} catch (Exception e) {
					continue;
				}
			}
			tx = commitTx(tx);
		} finally {
			rollbackTx(tx);
			pm.close();
		}
	}

	void checkOneForFinish(RunConnection rc, RemoteRunDelegate run) {
		if (run == null)
			return;
		if (run.doneTransitionToFinished || run.getStatus() != Status.Finished)
			return;
		run.doneTransitionToFinished = true;
		rc.setFinished(true);
		try {
			for (Listener l : run.getListeners())
				if (l.getName().equals("io")) {
					notifyFinished(run.id, l, run);
					return;
				}
		} catch (Exception e) {
			log.warn("failed to do notification of completion", e);
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
