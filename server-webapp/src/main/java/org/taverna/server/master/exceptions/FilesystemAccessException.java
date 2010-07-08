/**
 * 
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

@WebFault(name = "FilesystemAccessFault")
public class FilesystemAccessException extends Exception {
	private static final long serialVersionUID = 8715937300989820318L;

	public FilesystemAccessException(String msg) {
		super(msg);
	}

	public FilesystemAccessException(String string, Throwable cause) {
		super(string, cause);
	}
}