/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * An exception thrown when an operation is attempted which only the owner is
 * permitted to do. Notably, permissions may <i>only</i> be manipulated by the
 * owner.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NotOwnerFault")
@SuppressWarnings("serial")
public class NotOwnerException extends Exception {
	public NotOwnerException() {
		super("not permitted; not the owner");
	}
}
