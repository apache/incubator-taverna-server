/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;


/**
 * Exception that is thrown to indicate that the user is not permitted to
 * create something.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoCreateFault")
public class NoCreateException extends NoUpdateException {
	private static final long serialVersionUID = 270413810410167235L;

	public NoCreateException() {
		super("not permitted to create");
	}

	public NoCreateException(String string) {
		super(string);
	}

	public NoCreateException(String string, Throwable e) {
		super(string, e);
	}
}