/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Exception that is thrown to indicate that the state change requested for a
 * run is impossible.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoUpdateFault")
public class BadStateChangeException extends NoUpdateException {
	private static final long serialVersionUID = -4490826388447601775L;

	public BadStateChangeException() {
		super("cannot do that state change");
	}

	public BadStateChangeException(Throwable t) {
		super("cannot do that state change", t);
	}

	public BadStateChangeException(String msg, Throwable t) {
		super(msg, t);
	}

	public BadStateChangeException(String message) {
		super(message);
	}
}
