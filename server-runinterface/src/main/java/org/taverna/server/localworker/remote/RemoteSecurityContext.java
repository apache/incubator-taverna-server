/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Outline of the security context for a workflow run.
 * 
 * @author Donal Fellows
 */
public interface RemoteSecurityContext extends Remote {
	void setKeystore(@NonNull byte[] keystore) throws RemoteException, ImplementationException;

	void setPassword(@NonNull char[] password) throws RemoteException, ImplementationException;

	void setTruststore(@NonNull byte[] truststore) throws RemoteException, ImplementationException;

	void setUriToAliasMap(@NonNull HashMap<URI, String> uriToAliasMap)
			throws RemoteException;

	void setHelioToken(@NonNull String helioToken) throws RemoteException;
}
