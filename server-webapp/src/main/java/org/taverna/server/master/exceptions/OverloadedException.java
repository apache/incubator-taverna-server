/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Exception that is thrown to indicate that the state change requested for a
 * run is currently impossible due to excessive server load.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "OverloadedFault")
public class OverloadedException extends BadStateChangeException {
	private static final long serialVersionUID = 490826388447601776L;

	public OverloadedException() {
		super("server too busy; try later please");
	}

	public OverloadedException(Throwable t) {
		super("server too busy; try later please", t);
	}

	public OverloadedException(String msg, Throwable t) {
		super(msg, t);
	}

	public OverloadedException(String message) {
		super(message);
	}
}
