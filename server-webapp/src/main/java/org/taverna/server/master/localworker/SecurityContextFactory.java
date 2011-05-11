/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.UsernamePrincipal;
import org.taverna.server.master.utils.X500Utils;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Singleton factory. Really is a singleton (and is also very trivial); the
 * singleton-ness is just about limiting the number of instances of this around
 * even when lots of serialization is going on.
 * 
 * @see Serializable
 * @author Donal Fellows
 */
@SuppressWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
public class SecurityContextFactory implements
		org.taverna.server.master.interfaces.SecurityContextFactory {
	private static final long serialVersionUID = 12345678987654321L;
	private static SecurityContextFactory instance;
	transient RunDBSupport db;
	transient FilenameUtils fileUtils;
	transient X500Utils x500Utils;

	@SuppressWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
			"UPM_UNCALLED_PRIVATE_METHOD" })
	@java.lang.SuppressWarnings("unused")
	@PreDestroy
	private void closeLog() {
		instance = null;
	}

	@SuppressWarnings({ "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
			"UPM_UNCALLED_PRIVATE_METHOD" })
	@java.lang.SuppressWarnings("unused")
	@PostConstruct
	private void setAsSingleton() {
		instance = this;
	}

	@Required
	public void setRunDatabase(RunDBSupport db) {
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
	public SecurityContextDelegate create(RemoteRunDelegate run,
			UsernamePrincipal owner) throws Exception {
		return new SecurityContextDelegate(run, owner, this);
	}

	private Object readResolve() {
		return (instance != null) ? instance : (instance = this);
	}
}