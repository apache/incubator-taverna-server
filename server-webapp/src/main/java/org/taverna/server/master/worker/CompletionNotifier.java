/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;


/**
 * How to convert a notification about the completion of a job into a message.
 * 
 * @author Donal Fellows
 */
public interface CompletionNotifier {
	/**
	 * Called to get the content of a message that a workflow run has finished.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param run
	 *            What run are we talking about.
	 * @param code
	 *            What the exit code was.
	 * @return The plain-text content of the message.
	 */
	String makeCompletionMessage(String name, RemoteRunDelegate run, int code);

	/**
	 * Called to get the subject of the message to dispatch.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param run
	 *            What run are we talking about.
	 * @param code
	 *            What the exit code was.
	 * @return The plain-text subject of the message.
	 */
	String makeMessageSubject(String name, RemoteRunDelegate run, int code);
}
