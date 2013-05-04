/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import static java.util.Collections.emptyList;
import static org.taverna.server.master.identity.WorkflowInternalAuthProvider.PREFIX;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.taverna.server.master.common.Roles;
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
	Log log = LogFactory.getLog("Taverna.Server.Worker.Policy");
	private PolicyLimits limits;
	private RunDBSupport runDB;

	@Required
	public void setLimits(PolicyLimits limits) {
		this.limits = limits;
	}

	@Required
	public void setRunDB(RunDBSupport runDB) {
		this.runDB = runDB;
	}

	@Override
	public int getMaxRuns() {
		return limits.getMaxRuns();
	}

	@Override
	public Integer getMaxRuns(UsernamePrincipal user) {
		return null;
	}

	@Override
	public int getOperatingLimit() {
		return limits.getOperatingLimit();
	}

	@Override
	public List<Workflow> listPermittedWorkflows(UsernamePrincipal user) {
		return emptyList();
	}

	private boolean isSelfAccess(String runId) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		boolean self = false;
		String id = null;
		for (GrantedAuthority a : auth.getAuthorities()) {
			String aa = a.getAuthority();
			if (aa.equals(Roles.SELF)) {
				self = true;
				continue;
			}
			if (!aa.startsWith(PREFIX))
				continue;
			id = aa.substring(PREFIX.length());
		}
		return self && runId.equals(id);
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
		if (isSelfAccess(run.getId())) {
			if (log.isDebugEnabled())
				log.debug("access by workflow to itself: " + run.getId());
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
		if (isSelfAccess(run.getId())) {
			if (log.isDebugEnabled())
				log.debug("update access by workflow to itself: " + run.getId());
			return;
		}
		if (!context.getPermittedUpdaters().contains(user.getName()))
			throw new NoUpdateException(
					"workflow run not owned by you and you're not granted access");
	}
}
