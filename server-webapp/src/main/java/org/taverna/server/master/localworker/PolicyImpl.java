/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.Collections.emptyList;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;

/**
 * Basic policy implementation that allows any workflow to be instantiated by
 * any user, but which does not permit users to access each others workflow
 * runs. It also imposes a global limit on the number of workflow runs at once.
 * 
 * @author Donal Fellows
 */
class PolicyImpl implements Policy {
	private LocalWorkerState state;
	private RunDatabase runDB;

	@Required
	public void setState(LocalWorkerState state) {
		this.state = state;
	}

	@Required
	public void setRunDB(RunDatabase runDB) {
		this.runDB = runDB;
	}

	@Override
	public int getMaxRuns() {
		return state.getMaxRuns();
	}

	@Override
	public Integer getMaxRuns(Principal user) {
		return null;
	}

	@Override
	public List<Workflow> listPermittedWorkflows(Principal user) {
		return emptyList();
	}

	@Override
	public boolean permitAccess(Principal user, TavernaRun run) {
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner().equals(user))
			return true;
		return context.getPermittedReaders().contains(user.getName());
	}

	@Override
	public synchronized void permitCreate(Principal user, Workflow workflow)
			throws NoCreateException {
		if (user == null)
			throw new NoCreateException(
					"anonymous workflow creation not allowed");
		if (runDB.countRuns() >= getMaxRuns())
			throw new NoCreateException("server load exceeded; please wait");
	}

	@Override
	public synchronized void permitDestroy(Principal user, TavernaRun run)
			throws NoDestroyException {
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner() == null || context.getOwner().equals(user))
			return;
		if (user == null
				|| !context.getPermittedDestroyers().contains(user.getName()))
			throw new NoDestroyException();
	}

	@Override
	public void permitUpdate(Principal user, TavernaRun run)
			throws NoUpdateException {
		TavernaSecurityContext context = run.getSecurityContext();
		if (context.getOwner().equals(user))
			return;
		if (user == null
				|| !context.getPermittedUpdaters().contains(user.getName()))
			throw new NoUpdateException(
					"workflow run not owned by you and you're not granted access");
	}
}
