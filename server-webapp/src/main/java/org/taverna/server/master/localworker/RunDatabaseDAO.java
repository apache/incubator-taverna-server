/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static org.taverna.server.master.localworker.RunConnection.toDBform;

import java.io.IOException;
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
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.JDOSupport;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * This handles storing runs, interfacing with the underlying state engine as
 * necessary.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class RunDatabaseDAO extends JDOSupport<RunConnection> {
	public RunDatabaseDAO() {
		super(RunConnection.class);
	}

	private Log log = LogFactory.getLog("Taverna.Server.LocalWorker.RunDB");
	private RunDatabase facade;

	@Required
	public void setFacade(RunDatabase facade) {
		this.facade = facade;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

	@SuppressWarnings("unchecked")
	private List<String> nameRuns() {
		log.debug("fetching all run names");
		return (List<String>) namedQuery("names").execute();
	}

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
			return (rc == null) ? null : rc.fromDBform(facade);
		} catch (Exception e) {
			return null;
		}
	}

	@WithinSingleTransaction
	public Map<String, TavernaRun> listRuns(UsernamePrincipal user, Policy p) {
		Map<String, TavernaRun> result = new HashMap<String, TavernaRun>();
		for (String id : nameRuns()) {
			try {
				RemoteRunDelegate rrd = pickRun(id).fromDBform(facade);
				if (p.permitAccess(user, rrd))
					result.put(id, rrd);
			} catch (Exception e) {
				continue;
			}
		}
		return result;
	}

	@WithinSingleTransaction
	public List<String> listRunNames() {
		ArrayList<String> runNames = new ArrayList<String>();
		for (RunConnection rc : allRuns()) {
			if (rc.getId() != null)
				runNames.add(rc.getId());
		}
		return runNames;
	}

	@WithinSingleTransaction
	public RemoteRunDelegate pickArbitraryRun() throws Exception {
		for (RunConnection rc : allRuns()) {
			if (rc.getId() == null)
				continue;
			return rc.fromDBform(facade);
		}
		return null;
	}

	@WithinSingleTransaction
	public void persistRun(RemoteRunDelegate rrd) throws IOException {
		persist(rrd);
	}

	@WithinSingleTransaction
	public void unpersistRun(String name) {
		RunConnection rc = pickRun(name);
		if (rc != null)
			delete(rc);
	}

	@WithinSingleTransaction
	public void flushToDisk(RemoteRunDelegate run) throws IOException {
			getById(run.id).makeChanges(run);
	}

	@WithinSingleTransaction
	public void doClean() {
		log.debug("deleting runs that timed out before " + new Date());
		List<String> toDelete = expiredRuns();
		log.debug("found " + toDelete.size() + " runs to delete");
		for (String id : toDelete)
			delete(getById(id));
	}

	@WithinSingleTransaction
	public List<RemoteRunDelegate> getNotifiable() {
		List<RemoteRunDelegate> toNotify = new ArrayList<RemoteRunDelegate>();
		for (RunConnection rc : allRuns()) {
			try {
				RemoteRunDelegate rrd = rc.fromDBform(facade);
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
}
