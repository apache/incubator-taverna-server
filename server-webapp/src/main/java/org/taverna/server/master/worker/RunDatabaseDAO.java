/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import static org.taverna.server.master.worker.RunConnection.toDBform;

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

	private Log log = LogFactory.getLog("Taverna.Server.Worker.RunDB");
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

	/**
	 * @return The number of workflow runs in the database.
	 */
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

	@WithinSingleTransaction
	public String getSecurityToken(String name) {
		RunConnection rc = getById(name);
		return rc.getSecurityToken();
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

	/**
	 * Obtain a workflow run handle.
	 * 
	 * @param name
	 *            The identifier of the run.
	 * @return The run handle, or <tt>null</tt> if there is no such run.
	 */
	@WithinSingleTransaction
	public TavernaRun get(String name) {
		try {
			RunConnection rc = pickRun(name);
			return (rc == null) ? null : rc.fromDBform(facade);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the runs that a user can read things from.
	 * 
	 * @param user
	 *            Who is asking?
	 * @param p
	 *            The policy that determines what they can see.
	 * @return A mapping from run IDs to run handles.
	 */
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

	/**
	 * @return A list of the IDs for all workflow runs.
	 */
	@WithinSingleTransaction
	public List<String> listRunNames() {
		ArrayList<String> runNames = new ArrayList<String>();
		for (RunConnection rc : allRuns()) {
			if (rc.getId() != null)
				runNames.add(rc.getId());
		}
		return runNames;
	}

	/**
	 * @return An arbitrary, representative workflow run.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	@WithinSingleTransaction
	public RemoteRunDelegate pickArbitraryRun() throws Exception {
		for (RunConnection rc : allRuns()) {
			if (rc.getId() == null)
				continue;
			return rc.fromDBform(facade);
		}
		return null;
	}

	/**
	 * Make a workflow run persistent. Must only be called once per workflow
	 * run.
	 * 
	 * @param rrd
	 *            The workflow run to persist.
	 * @throws IOException
	 *             If anything goes wrong with serialisation of the run.
	 */
	@WithinSingleTransaction
	public void persistRun(RemoteRunDelegate rrd) throws IOException {
		persist(rrd);
	}

	/**
	 * Stop a workflow run from being persistent.
	 * 
	 * @param name
	 *            The ID of the run.
	 */
	@WithinSingleTransaction
	public void unpersistRun(String name) {
		RunConnection rc = pickRun(name);
		if (rc != null)
			delete(rc);
	}

	/**
	 * Ensure that the given workflow run is synchronized with the database.
	 * 
	 * @param run
	 *            The run to synchronise.
	 * @throws IOException
	 *             If serialization of anything fails.
	 */
	@WithinSingleTransaction
	public void flushToDisk(RemoteRunDelegate run) throws IOException {
		getById(run.id).makeChanges(run);
	}

	/**
	 * Remove all workflow runs that have expired.
	 */
	@WithinSingleTransaction
	public void doClean() {
		log.debug("deleting runs that timed out before " + new Date());
		List<String> toDelete = expiredRuns();
		log.debug("found " + toDelete.size() + " runs to delete");
		for (String id : toDelete) {
			RunConnection rc = getById(id);
			try {
				rc.fromDBform(facade).run.destroy();
			} catch (Exception e) {
				log.debug("failed to delete execution resource for " + id, e);
			}
			delete(rc);
		}
	}

	/**
	 * @return A list of workflow runs that are candidates for doing
	 *         notification of termination.
	 */
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
