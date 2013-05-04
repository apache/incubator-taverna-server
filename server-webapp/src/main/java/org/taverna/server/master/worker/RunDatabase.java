/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.notification.NotificationEngine;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * The main facade bean that interfaces to the database of runs.
 * 
 * @author Donal Fellows
 */
public class RunDatabase implements RunStore, RunDBSupport {
	private Log log = LogFactory.getLog("Taverna.Server.Worker.RunDB");
	RunDatabaseDAO dao;
	private List<CompletionNotifier> notifier = new ArrayList<CompletionNotifier>();
	private NotificationEngine notificationEngine;
	@Autowired
	private FactoryBean factory;

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
	public void setDao(RunDatabaseDAO dao) {
		this.dao = dao;
	}

	@Override
	public void checkForFinishNow() {
		for (RemoteRunDelegate rrd : dao.getNotifiable())
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

	@Override
	public void cleanNow() {
		try {
			dao.doClean();
		} catch (Exception e) {
			log.warn("failure during deletion of expired runs", e);
		}
	}

	@Override
	public int countRuns() {
		return dao.countRuns();
	}

	@Override
	public void flushToDisk(RemoteRunDelegate run) {
		try {
			dao.flushToDisk(run);
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
	}

	@Override
	public RemoteRunDelegate pickArbitraryRun() throws Exception {
		return dao.pickArbitraryRun();
	}

	@Override
	public List<String> listRunNames() {
		return dao.listRunNames();
	}

	@Override
	public TavernaRun getRun(UsernamePrincipal user, Policy p, String uuid)
			throws UnknownRunException {
		// Check first to see if the 'uuid' actually looks like a UUID; if
		// not, throw it out immediately without logging an exception.
		try {
			UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			log.debug("run ID does not look like UUID; rejecting...");
			throw new UnknownRunException();
		}
		TavernaRun run = dao.get(uuid);
		if (run != null && (user == null || p.permitAccess(user, run)))
			return run;
		throw new UnknownRunException();
	}

	@Override
	public TavernaRun getRun(String uuid) throws UnknownRunException {
		TavernaRun run = dao.get(uuid);
		if (run != null)
			return run;
		throw new UnknownRunException();
	}

	@Override
	public Map<String, TavernaRun> listRuns(UsernamePrincipal user, Policy p) {
		return dao.listRuns(user, p);
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
			dao.persistRun(rrd);
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected problem when persisting run record in database",
					e);
		}
		return rrd.getId();
	}

	@Override
	public void unregisterRun(String uuid) {
		try {
			dao.unpersistRun(uuid);
		} catch (RuntimeException e) {
			log.debug("problem persisting the deletion of the run " + uuid, e);
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

	@Override
	public FactoryBean getFactory() {
		return factory;
	}
}
