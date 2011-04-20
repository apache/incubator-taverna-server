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
import org.taverna.server.master.utils.JDOSupport;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class RunDatabaseDAO extends JDOSupport<RunConnection> implements
		RunStore, RunDBSupport {
	public RunDatabaseDAO() {
		super(RunConnection.class);
	}

	private Log log = LogFactory.getLog("Taverna.Server.LocalWorker.RunDB");
	private List<CompletionNotifier> notifier = new ArrayList<CompletionNotifier>();
	private NotificationEngine notificationEngine;
	private RunDatabaseDAO self;

	@Override
	public void setNotifier(CompletionNotifier n) {
		notifier = asList(n);
	}

	@Required
	@Override
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	@Required
	public void setSelf(RunDatabaseDAO self) {
		this.self = self;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	@SuppressWarnings("unchecked")
	private List<String> nameRuns() {
		log.debug("fetching all run names");
		return (List<String>) namedQuery("names").execute();
	}

	@Override
	@WithinSingleTransaction
	public int countRuns() {
		log.debug("counting the number of runs");
		return (Integer) namedQuery("count").execute();
	}

	@SuppressWarnings("unchecked")
	private List<String> expiredRuns() {
		return (List<String>) namedQuery("timedout").execute();
	}

	private RunConnection pickRun(String name) {
		log.debug("fetching the run called " + name);
		try {
			RunConnection rc = getById(name);
			if (rc == null)
				log.warn("no result for " + name);
			return rc;
		} catch (RuntimeException e) {
			log.warn("problem in fetch", e);
			throw e;
		}
	}

	private void persist(RemoteRunDelegate rrd) throws IOException {
		persist(toDBform(rrd));
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

	@WithinSingleTransaction
	public TavernaRun get(String name) {
		try {
			RunConnection rc = pickRun(name);
			return (rc == null) ? null : rc.fromDBform(this);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public TavernaRun getRun(UsernamePrincipal user, Policy p, String uuid)
			throws UnknownRunException {
		TavernaRun run = self.get(uuid);
		if (run != null && (user == null || p.permitAccess(user, run)))
			return run;
		throw new UnknownRunException();
	}

	@Override
	public TavernaRun getRun(String uuid) throws UnknownRunException {
		TavernaRun run = self.get(uuid);
		if (run != null)
			return run;
		throw new UnknownRunException();
	}

	@Override
	@WithinSingleTransaction
	public Map<String, TavernaRun> listRuns(UsernamePrincipal user, Policy p) {
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
	}

	@Override
	@WithinSingleTransaction
	public List<String> listRunNames() {
		ArrayList<String> runNames = new ArrayList<String>();
		for (RunConnection rc : allRuns()) {
			if (rc.getId() != null)
				runNames.add(rc.getId());
		}
		return runNames;
	}

	@Override
	@WithinSingleTransaction
	public RemoteRunDelegate pickArbitraryRun() throws Exception {
		for (RunConnection rc : allRuns()) {
			if (rc.getId() == null)
				continue;
			return rc.fromDBform(this);
		}
		return null;
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
		try {
			self.persistRun(rrd);
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
		return rrd.getId();
	}

	@WithinSingleTransaction
	public void persistRun(RemoteRunDelegate rrd) throws IOException {
		persist(rrd);
	}

	@Override
	public void unregisterRun(String name) {
		try {
			self.unpersistRun(name);
		} catch (RuntimeException e) {
			log.debug("problem persisting the deletion of the run " + name, e);
		}
	}

	@WithinSingleTransaction
	public void unpersistRun(String name) {
		RunConnection rc = pickRun(name);
		if (rc != null)
			delete(rc);
	}

	@Override
	@WithinSingleTransaction
	public void flushToDisk(RemoteRunDelegate run) {
		try {
			getById(run.id).makeChanges(run);
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
	}

	@Override
	public void cleanNow() {
		try {
			self.doClean();
		} catch (Exception e) {
			log.warn("failure during deletion of expired runs", e);
		}
	}

	@WithinSingleTransaction
	public void doClean() {
		log.debug("deleting runs that timed out before " + new Date());
		List<String> toDelete = expiredRuns();
		log.debug("found " + toDelete.size() + " runs to delete");
		for (String id : toDelete)
			delete(getById(id));
	}

	@Override
	public void checkForFinishNow() {
		for (RemoteRunDelegate rrd : self.getNotifiable())
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

	@WithinSingleTransaction
	public List<RemoteRunDelegate> getNotifiable() {
		List<RemoteRunDelegate> toNotify = new ArrayList<RemoteRunDelegate>();
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
		return toNotify;
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
