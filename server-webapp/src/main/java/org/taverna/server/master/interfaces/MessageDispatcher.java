/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.interfaces;

public interface MessageDispatcher {
	/**
	 * @return Whether this message dispatcher is actually available (fully
	 *         configured, etc.)
	 */
	boolean isAvailable();

	/**
	 * Dispatch a message to a recipient.
	 * 
	 * @param messageSubject
	 *            The subject of the message to send.
	 * @param messageContent
	 *            The plain-text content of the message to send.
	 * @param targetParameter
	 *            A description of where it is to go.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception;
}