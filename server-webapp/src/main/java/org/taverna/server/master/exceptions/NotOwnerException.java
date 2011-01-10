/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

@WebFault(name = "NotOwnerFault")
public class NotOwnerException extends Exception {
	public NotOwnerException() {
		super("not permitted; not the owner");
	}
}
