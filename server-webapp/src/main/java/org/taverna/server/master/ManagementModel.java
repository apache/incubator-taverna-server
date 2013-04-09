/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
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
	boolean getAllowNewWorkflowRuns();

	/**
	 * @return whether we should log all workflows sent to us.
	 */
	boolean getLogIncomingWorkflows();

	/**
	 * @return whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	boolean getLogOutgoingExceptions();

	/**
	 * @return the file that all usage records should be appended to, or
	 *         <tt>null</tt> if they should be just dropped.
	 */
	String getUsageRecordLogFile();

	/**
	 * @param logIncomingWorkflows
	 *            whether we should log all workflows sent to us.
	 */
	void setLogIncomingWorkflows(boolean logIncomingWorkflows);

	/**
	 * @param allowNewWorkflowRuns
	 *            whether we allow the creation of new workflow runs.
	 */
	void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns);

	/**
	 * @param logOutgoingExceptions
	 *            whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	void setLogOutgoingExceptions(boolean logOutgoingExceptions);

	/**
	 * @param usageRecordLogFile
	 *            the file that all usage records should be appended to, or
	 *            <tt>null</tt> if they should be just dropped.
	 */
	void setUsageRecordLogFile(String usageRecordLogFile);
}
