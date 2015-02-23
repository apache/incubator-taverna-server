/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

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
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.UriBuilderFactory;
import org.taverna.server.master.utils.CertificateChainFetcher;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.UsernamePrincipal;
import org.taverna.server.master.utils.X500Utils;

/**
 * Singleton factory. Really is a singleton (and is also very trivial); the
 * singleton-ness is just about limiting the number of instances of this around
 * even when lots of serialization is going on.
 * 
 * @see Serializable
 * @author Donal Fellows
 */
public class SecurityContextFactory implements
		org.taverna.server.master.interfaces.SecurityContextFactory {
	private static final long serialVersionUID = 12345678987654321L;
	private static SecurityContextFactory instance;
	transient RunDBSupport db;
	transient FilenameUtils fileUtils;
	transient X500Utils x500Utils;
	transient UriBuilderFactory uriSource;
	transient CertificateChainFetcher certFetcher;
	transient String httpRealm;
	private transient PasswordIssuer passwordIssuer;
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
		return getLog("Taverna.Server.Worker.Security");
	}

	private void installAsInstance(SecurityContextFactory handle) {
		instance = handle;
	}

	@PreDestroy
	void removeAsSingleton() {
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

	@PostConstruct
	void setAsSingleton() {
		installAsInstance(this);
		if (getProvider(PROVIDER_NAME) == null)
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

	@Required
	public void setRunDatabase(RunDBSupport db) {
		this.db = db;
	}

	@Required
	public void setCertificateFetcher(CertificateChainFetcher fetcher) {
		this.certFetcher = fetcher;
	}

	@Required
	public void setFilenameConverter(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	@Required
	public void setX500Utils(X500Utils x500Utils) {
		this.x500Utils = x500Utils;
	}

	@Required
	public void setUriSource(UriBuilderFactory uriSource) {
		this.uriSource = uriSource;
	}

	@Required
	public void setHttpRealm(String realm) {
		this.httpRealm = realm; //${http.realmName}
	}

	@Required
	public void setPasswordIssuer(PasswordIssuer issuer) {
		this.passwordIssuer = issuer;
	}

	@Override
	public SecurityContextDelegate create(TavernaRun run,
			UsernamePrincipal owner) throws Exception {
		Log log = log();
		if (log.isDebugEnabled())
			log.debug("constructing security context delegate for " + owner);
		RemoteRunDelegate rrd = (RemoteRunDelegate) run;
		return new HelioSecurityContextDelegateImpl(rrd, owner, this);
	}

	private Object readResolve() {
		if (instance == null)
			installAsInstance(this);
		return instance;
	}

	public String issueNewPassword() {
		return passwordIssuer.issue();
	}
}