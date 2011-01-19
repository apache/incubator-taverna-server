/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

/**
 * How a user should be notified about the completion of a job.
 * 
 * @author Donal Fellows
 */
public interface CompletionNotifier {
	/**
	 * Called to notify someone or something that a workflow run has finished.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param run
	 *            What run are we talking about.
	 * @param code
	 *            What the exit code was.
	 * @return The content of the message.
	 */
	String notifyComplete(String name, RemoteRunDelegate run, int code);

	/**
	 * @return What mechanism to dispatch by, or <tt>null</tt> for all of them.
	 */
	String getTargetDispatcher();
}
