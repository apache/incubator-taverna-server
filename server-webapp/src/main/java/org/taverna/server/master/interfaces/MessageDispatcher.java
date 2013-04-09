/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The interface supported by all notification message dispatchers.
 * @author Donal Fellows
 */
public interface MessageDispatcher {
	/**
	 * @return Whether this message dispatcher is actually available (fully
	 *         configured, etc.)
	 */
	boolean isAvailable();

	/**
	 * Dispatch a message to a recipient.
	 * 
	 * @param originator
	 *            The workflow run that produced the message.
	 * @param messageSubject
	 *            The subject of the message to send.
	 * @param messageContent
	 *            The plain-text content of the message to send.
	 * @param targetParameter
	 *            A description of where it is to go.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	void dispatch(@NonNull TavernaRun originator,
			@NonNull String messageSubject, @NonNull String messageContent,
			@NonNull String targetParameter) throws Exception;
}