/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import javax.xml.ws.WebFault;

/**
 * Exception that indicates where a change of a workflow run's status is
 * illegal.
 * 
 * @author Donal Fellows
 * @see RemoteSingleRun#setStatus(RemoteStatus)
 */
@WebFault(name = "IllegalStateTransitionFault", targetNamespace = "http://ns.taverna.org.uk/2010/xml/server/worker/")
public class IllegalStateTransitionException extends Exception {
	private static final long serialVersionUID = 159673249162345L;

	public IllegalStateTransitionException() {
		this("illegal state transition");
	}

	public IllegalStateTransitionException(String message) {
		super(message);
	}

	public IllegalStateTransitionException(Throwable cause) {
		this("illegal state transition", cause);
	}

	public IllegalStateTransitionException(String message, Throwable cause) {
		super(message, cause);
	}
}
