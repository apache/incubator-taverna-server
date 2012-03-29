/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.security.Security.addProvider;
import static java.security.Security.getProvider;
import static java.security.Security.removeProvider;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
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
	private transient BouncyCastleProvider provider;

	/**
	 * Whether to support HELIO CIS tokens.
	 */
	@Value("${helio.cis.enableTokenPassing}")
	boolean supportHelioToken;

	/**
	 * Whether to log the details of security (passwords, etc).
	 */
	@Value("${log.security.details}")
	boolean logSecurityDetails;

	private Log log() {
		return getLog("Taverna.Server.LocalWorker.Security");
	}

	@SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
	private void installAsInstance(SecurityContextFactory handle) {
		instance = handle;
	}

	@SuppressWarnings("UPM_UNCALLED_PRIVATE_METHOD")
	@java.lang.SuppressWarnings("unused")
	@PreDestroy
	private void closeLog() {
		installAsInstance(null);
		try {
			if (provider != null)
				removeProvider(provider.getName());
		} catch (SecurityException e) {
			log().warn(
					"failed to remove BouncyCastle security provider; "
							+ "might be OK if configured in environment", e);
		}
	}

	@SuppressWarnings("UPM_UNCALLED_PRIVATE_METHOD")
	@java.lang.SuppressWarnings("unused")
	@PostConstruct
	private void setAsSingleton() {
		installAsInstance(this);
		if (getProvider(PROVIDER_NAME) == null) {
			try {
				provider = new BouncyCastleProvider();
				if (addProvider(provider) == -1)
					provider = null;
			} catch (SecurityException e) {
				log().warn(
						"failed to install BouncyCastle security provider; "
								+ "might be OK if already configured", e);
				provider = null;
			}
		}
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
		log().debug("constructing security context delegate for " + owner);
		return new SecurityContextDelegateImpl(run, owner, this);
	}

	private Object readResolve() {
		if (instance == null)
			installAsInstance(this);
		return instance;
	}
}