/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

/**
 * An exception that is thrown to indicate that a credential-descriptor or
 * trust-descriptor supplied as part of a credential or trust management
 * operation is invalid.
 * 
 * @author Donal Fellows
 * 
 */
@SuppressWarnings("serial")
public class InvalidCredentialException extends Exception {
	private static final String MSG = "that credential is invalid";

	public InvalidCredentialException() {
		super(MSG);
	}

	public InvalidCredentialException(String reason) {
		super(MSG + ": " + reason);
	}

	public InvalidCredentialException(String reason, Throwable cause) {
		this(reason);
		initCause(cause);
	}

	public InvalidCredentialException(Throwable cause) {
		this(cause.getMessage(), cause);
	}
}
