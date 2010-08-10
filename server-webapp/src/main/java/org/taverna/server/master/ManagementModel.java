package org.taverna.server.master;

public class ManagementModel {
	/** Whether we should log all workflows sent to us. */
	private boolean logIncomingWorkflows = false;

	/** Whether we allow the creation of new workflow runs. */
	private boolean allowNewWorkflowRuns = true;

	/**
	 * Whether outgoing exceptions should be logged before being converted to
	 * responses.
	 */
	private boolean logOutgoingExceptions = false;

	/**
	 * @param logIncomingWorkflows
	 *            whether we should log all workflows sent to us.
	 */
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		this.logIncomingWorkflows = logIncomingWorkflows;
	}

	/**
	 * @return whether we should log all workflows sent to us.
	 */
	public boolean getLogIncomingWorkflows() {
		return logIncomingWorkflows;
	}

	/**
	 * @param allowNewWorkflowRuns
	 *            whether we allow the creation of new workflow runs.
	 */
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
		this.allowNewWorkflowRuns = allowNewWorkflowRuns;
	}

	/**
	 * @return whether we allow the creation of new workflow runs.
	 */
	public boolean getAllowNewWorkflowRuns() {
		return allowNewWorkflowRuns;
	}

	/**
	 * @param logOutgoingExceptions
	 *            whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
		this.logOutgoingExceptions = logOutgoingExceptions;
	}

	/**
	 * @return whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	public boolean getLogOutgoingExceptions() {
		return logOutgoingExceptions;
	}
}
