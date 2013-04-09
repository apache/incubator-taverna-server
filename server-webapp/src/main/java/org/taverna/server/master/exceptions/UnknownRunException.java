/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Exception thrown to indicate that the handle of the run is unknown (or
 * unacceptable to the current user).
 * 
 * @author Donal Fellows
 */
@WebFault(name = "UnknownRunFault")
public class UnknownRunException extends Exception {
	private static final long serialVersionUID = -3028749401786242841L;

	public UnknownRunException() {
		super("unknown run UUID");
	}
}