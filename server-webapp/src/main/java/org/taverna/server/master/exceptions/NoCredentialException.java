/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.exceptions;

public class NoCredentialException extends Exception {
	public NoCredentialException() {
		super("no such credential");
	}
}
