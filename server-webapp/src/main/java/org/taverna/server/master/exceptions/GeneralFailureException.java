/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import static org.taverna.server.master.common.Namespaces.SERVER_SOAP;

import javax.xml.ws.WebFault;

/**
 * Some sort of exception that occurred which we can't map any other way. This
 * is generally indicative of a problem server-side.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "GeneralFailureFault", targetNamespace = SERVER_SOAP)
@SuppressWarnings("serial")
public class GeneralFailureException extends RuntimeException {
	public GeneralFailureException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public GeneralFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
