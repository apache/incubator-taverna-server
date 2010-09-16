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
