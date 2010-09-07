package org.taverna.server.master;

/**
 * The model of the webapp's state Java Bean.
 * 
 * @author Donal Fellows
 */
public interface ManagementModel {
	/**
	 * @return whether we allow the creation of new workflow runs.
	 */
	public boolean getAllowNewWorkflowRuns();

	/**
	 * @return whether we should log all workflows sent to us.
	 */
	public boolean getLogIncomingWorkflows();

	/**
	 * @return whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	public boolean getLogOutgoingExceptions();

	/**
	 * @param logIncomingWorkflows
	 *            whether we should log all workflows sent to us.
	 */
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows);

	/**
	 * @param allowNewWorkflowRuns
	 *            whether we allow the creation of new workflow runs.
	 */
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns);

	/**
	 * @param logOutgoingExceptions
	 *            whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	public void setLogOutgoingExceptions(boolean logOutgoingExceptions);
}
