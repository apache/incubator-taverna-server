/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Indicates that the port name was not recognized.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "BadInputPortNameFault")
public class BadInputPortNameException extends Exception {
	public BadInputPortNameException(String msg) {
		super(msg);
	}
}
