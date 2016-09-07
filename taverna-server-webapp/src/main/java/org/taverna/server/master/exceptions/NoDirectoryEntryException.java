/*
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Indicates that the file or directory name was not recognized.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoDirectoryEntryFault")
@SuppressWarnings("serial")
public class NoDirectoryEntryException extends Exception {
	public NoDirectoryEntryException(String msg) {
		super(msg);
	}
	public NoDirectoryEntryException(String msg,Exception cause) {
		super(msg, cause);
	}
}
