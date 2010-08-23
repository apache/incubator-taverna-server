package org.taverna.server.master.localworker;

import static java.util.Collections.emptyList;

import java.security.Principal;
import java.util.List;

import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * Basic policy implementation that allows any workflow to be instantiated by
 * any user, but which does not permit users to access each others workflow
 * runs. It also imposes a global limit on the number of workflow runs at once.
 * 
 * @author Donal Fellows
 */
class PolicyImpl implements Policy {
	private LocalWorkerState state;

	/**
	 * @param state the state to set
	 */
	public void setState(LocalWorkerState state) {
		this.state = state;
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
		return true;
	}

	@Override
	public synchronized void permitCreate(Principal user, Workflow workflow)
			throws NoCreateException {
		if (user == null)
			throw new NoCreateException(
					"anonymous workflow creation not allowed");
		if (state.countRuns() >= getMaxRuns())
			throw new NoCreateException("server load exceeded; please wait");
	}

	@Override
	public synchronized void permitDestroy(Principal user, TavernaRun run)
			throws NoDestroyException {
		// Simple model: if you can update, you can destroy
		try {
			permitUpdate(user, run);
		} catch (NoUpdateException e) {
			throw new NoDestroyException();
		}
	}

	@Override
	public void permitUpdate(Principal user, TavernaRun run)
			throws NoUpdateException {
		Principal owner = run.getSecurityContext().getOwner();
		if (owner == null)
			return; // Not owned by anyone; fair game
		if (user == null)
			throw new NoUpdateException("who are you?");
		if (!owner.getName().equals(user.getName()))
			throw new NoUpdateException("workflow run not owned by you");
	}
}
