/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.WebFault;

/**
 * Exception thrown to indicate that no listener by that name exists, or that
 * some other problem with listeners has occurred.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoListenerFault")
@XmlSeeAlso(BadPropertyValueException.class)
public class NoListenerException extends Exception {
	private static final long serialVersionUID = -2550897312787546547L;

	public NoListenerException() {
		super("no such listener");
	}

	public NoListenerException(String msg) {
		super(msg);
	}

	public NoListenerException(String msg, Throwable t) {
		super(msg, t);
	}
}