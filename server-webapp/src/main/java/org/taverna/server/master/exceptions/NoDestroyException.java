/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;


/**
 * Exception that is thrown to indicate that the user is not permitted to
 * destroy something.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoDestroyFault")
public class NoDestroyException extends NoUpdateException {
	private static final long serialVersionUID = 6207448533265237933L;

	public NoDestroyException() {
		super("not permitted to destroy");
	}
}