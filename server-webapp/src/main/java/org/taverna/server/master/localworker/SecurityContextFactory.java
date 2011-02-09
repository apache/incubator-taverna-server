package org.taverna.server.master.localworker;

import java.io.Serializable;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.utils.FilenameUtils;

/**
 * Singleton factory. Really is a singleton (and is also very trivial); the
 * singleton-ness is just about limiting the number of instances of this
 * around even when lots of serialization is going on.
 * 
 * @see Serializable
 * @author Donal Fellows
 */
public class SecurityContextFactory implements org.taverna.server.master.interfaces.SecurityContextFactory {
	private static SecurityContextFactory instance;
	transient RunDatabase db;
	transient FilenameUtils fileUtils;

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

	@Override
	public SecurityContextDelegate create(RemoteRunDelegate run,
			Principal owner) throws Exception {
		return new SecurityContextDelegate(run, owner, this);
	}

	private Object readResolve() {
		return (instance != null) ? instance : (instance = this);
	}
}