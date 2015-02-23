/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.ws.WebFault;

/**
 * Indicates a bad property value.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "BadPropertyValueFault")
public class BadPropertyValueException extends NoListenerException {
	private static final long serialVersionUID = -8459491388504556875L;

	public BadPropertyValueException(String msg) {
		super(msg);
	}

	public BadPropertyValueException(String msg, Throwable e) {
		super(msg, e);
	}
}
