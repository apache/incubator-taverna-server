/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Outline of the security context for a workflow run.
 * 
 * @author Donal Fellows
 */
public interface RemoteSecurityContext extends Remote {
	void setKeystore(@Nonnull byte[] keystore) throws RemoteException,
			ImplementationException;

	void setPassword(@Nonnull char[] password) throws RemoteException,
			ImplementationException;

	void setTruststore(@Nonnull byte[] truststore) throws RemoteException,
			ImplementationException;

	void setUriToAliasMap(@Nonnull Map<URI, String> uriToAliasMap)
			throws RemoteException;

	void setHelioToken(@Nonnull String helioToken) throws RemoteException;
}
