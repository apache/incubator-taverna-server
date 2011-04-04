/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.utils.UsernamePrincipal;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Basic policy implementation that allows any workflow to be instantiated by
 * any user, but which does not permit users to access each others workflow
 * runs. It also imposes a global limit on the number of workflow runs at once.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings("IS2_INCONSISTENT_SYNC")
class PolicyImpl implements Policy {
	Log log = LogFactory.getLog("Taverna.Server.LocalWorker.Policy");
	private LocalWorkerState state;
	private RunDBSupport runDB;

	@Required
	public void setState(LocalWorkerState state) {
		this.state = state;
	}

	@Required
	public void setRunDB(RunDBSupport runDB) {
		this.runDB = runDB;
	}

	@Override
	public int getMaxRuns() {
		return state.getMaxRuns();
	}

	@Override
	public Integer getMaxRuns(UsernamePrincipal user) {
		return null;
	}

	@Override
	public List<Workflow> listPermittedWorkflows(UsernamePrincipal user) {
		return emptyList();
	}

	@Override
	public boolean permitAccess(UsernamePrincipal user, TavernaRun run) {
		String username = user.getName();
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner().getName().equals(username)) {
			if (log.isDebugEnabled())
				log.debug("granted access by " + user.getName() + " to "
						+ run.getId());
			return true;
		}
		if (log.isDebugEnabled())
			log.debug("considering access by " + user.getName() + " to "
					+ run.getId());
		return context.getPermittedReaders().contains(username);
	}

	@Override
	public void permitCreate(UsernamePrincipal user, Workflow workflow)
			throws NoCreateException {
		if (user == null)
			throw new NoCreateException(
					"anonymous workflow creation not allowed");
		if (runDB.countRuns() >= getMaxRuns())
			throw new NoCreateException("server load exceeded; please wait");
	}

	@Override
	public synchronized void permitDestroy(UsernamePrincipal user, TavernaRun run)
			throws NoDestroyException {
		if (user == null)
			throw new NoDestroyException();
		String username = user.getName();
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner() == null
				|| context.getOwner().getName().equals(username))
			return;
		if (!context.getPermittedDestroyers().contains(username))
			throw new NoDestroyException();
	}

	@Override
	public void permitUpdate(UsernamePrincipal user, TavernaRun run)
			throws NoUpdateException {
		if (user == null)
			throw new NoUpdateException(
					"workflow run not owned by you and you're not granted access");
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner().getName().equals(user.getName()))
			return;
		if (!context.getPermittedUpdaters().contains(user.getName()))
			throw new NoUpdateException(
					"workflow run not owned by you and you're not granted access");
	}
}
