/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.exceptions;

import java.rmi.RemoteException;

import javax.xml.ws.WebFault;

/**
 * An exception that happened when the underlying filesystem was accessed.
 * @author Donal Fellows
 */
@WebFault(name = "FilesystemAccessFault")
public class FilesystemAccessException extends Exception {
	private static final long serialVersionUID = 8715937300989820318L;

	public FilesystemAccessException(String msg) {
		super(msg);
	}

	public FilesystemAccessException(String string, Throwable cause) {
		super(string, getRealCause(cause));
	}

	private static Throwable getRealCause(Throwable t) {
		if (t instanceof RemoteException) {
			RemoteException remote = (RemoteException) t;
			if (remote.detail != null)
				return remote.detail;
		}
		if (t.getCause() != null)
			return t.getCause();
		return t;
	}
}