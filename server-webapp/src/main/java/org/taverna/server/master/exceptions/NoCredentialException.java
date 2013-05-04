/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

/**
 * Exception that indicates the absence of an expected credential.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
public class NoCredentialException extends Exception {
	public NoCredentialException() {
		super("no such credential");
	}
}
