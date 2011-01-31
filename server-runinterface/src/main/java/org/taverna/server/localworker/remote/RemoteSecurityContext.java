/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Outline of the security context for a workflow run.
 * 
 * @author Donal Fellows
 */
public interface RemoteSecurityContext extends Remote {
	void setKeystore(byte[] keystore) throws RemoteException;

	void setPassword(char[] password) throws RemoteException;

	void setTruststore(byte[] truststore) throws RemoteException;

	void setUriToAliasMap(HashMap<URI, String> uriToAliasMap)
			throws RemoteException;
}
