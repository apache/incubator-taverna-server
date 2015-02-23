/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import javax.xml.ws.WebFault;

/**
 * Exception that indicates that the implementation has gone wrong in some
 * unexpected way.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "ImplementationFault", targetNamespace = "http://ns.taverna.org.uk/2010/xml/server/worker/")
@SuppressWarnings("serial")
public class ImplementationException extends Exception {
	public ImplementationException(String message) {
		super(message);
	}

	public ImplementationException(String message, Throwable cause) {
		super(message, cause);
	}
}
