/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import java.io.Serializable;
import java.security.Principal;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.X500Utils;

/**
 * Singleton factory. Really is a singleton (and is also very trivial); the
 * singleton-ness is just about limiting the number of instances of this around
 * even when lots of serialization is going on.
 * 
 * @see Serializable
 * @author Donal Fellows
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public class SecurityContextFactory implements
		org.taverna.server.master.interfaces.SecurityContextFactory {
	private static final long serialVersionUID = 12345678987654321L;
	private static SecurityContextFactory instance;
	transient RunDatabase db;
	transient FilenameUtils fileUtils;
	transient X500Utils x500Utils;

	@edu.umd.cs.findbugs.annotations.SuppressWarnings({
			"ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
			"UPM_UNCALLED_PRIVATE_METHOD" })
	@SuppressWarnings("unused")
	@PreDestroy
	private void closeLog() {
		instance = null;
	}

	public SecurityContextFactory() {
		if (instance == null)
			instance = this;
	}

	@Required
	public void setRunDatabase(RunDatabase db) {
		this.db = db;
	}

	@Required
	public void setFilenameConverter(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	@Required
	public void setX500Utils(X500Utils x500Utils) {
		this.x500Utils = x500Utils;
	}

	@Override
	public SecurityContextDelegate create(RemoteRunDelegate run, Principal owner)
			throws Exception {
		return new SecurityContextDelegate(run, owner, this);
	}

	private Object readResolve() {
		return (instance != null) ? instance : (instance = this);
	}
}