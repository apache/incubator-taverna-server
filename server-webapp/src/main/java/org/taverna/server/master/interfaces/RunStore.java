package org.taverna.server.master.interfaces;

import java.security.Principal;
import java.util.Map;

import org.taverna.server.master.exceptions.UnknownRunException;

/**
 * Interface to the mechanism that looks after the mapping of names to runs.
 * Instances of this class may also be responsible for enforcing timely cleanup
 * of expired workflows.
 * 
 * @author Donal Fellows.
 */
public interface RunStore {
	/**
	 * Obtain the workflow run for a given user and name.
	 * 
	 * @param user
	 *            Who wants to do the lookup.
	 * @param p
	 *            The general policy system context.
	 * @param uuid
	 *            The handle for the run.
	 * @return The workflow instance run.
	 * @throws UnknownRunException
	 *             If the lookup fails (either because it does not exist or
	 *             because it is not permitted for the user by the policy).
	 */
	public TavernaRun getRun(Principal user, Policy p, String uuid)
			throws UnknownRunException;

	/**
	 * Obtain the named workflow run.
	 * 
	 * @param uuid
	 *            The handle for the run.
	 * @return The workflow instance run.
	 * @throws UnknownRunException
	 *             If the lookup fails (either because it does not exist or
	 *             because it is not permitted for the user by the policy).
	 */
	public TavernaRun getRun(String uuid) throws UnknownRunException;

	/**
	 * List the runs that a particular user may access.
	 * 
	 * @param user
	 *            Who wants to do the lookup, or <code>null</code> if it is
	 *            being done "by the system" when the full mapping should be
	 *            returned.
	 * @param p
	 *            The general policy system context.
	 * @return A mapping from run names to run instances.
	 */
	public Map<String, TavernaRun> listRuns(Principal user, Policy p);

	/**
	 * Adds a workflow instance run to the store. Note that this operation is
	 * <i>not</i> expected to be security-checked; that is the callers'
	 * responsibility.
	 * 
	 * @param uuid
	 *            The name of the run.
	 * @param run
	 *            The run itself.
	 */
	public void registerRun(String uuid, TavernaRun run);

	/**
	 * Removes a run from the store. Note that this operation is <i>not</i>
	 * expected to be security-checked; that is the callers' responsibility.
	 * 
	 * @param uuid
	 *            The name of the run.
	 */
	public void unregisterRun(String uuid);
}
