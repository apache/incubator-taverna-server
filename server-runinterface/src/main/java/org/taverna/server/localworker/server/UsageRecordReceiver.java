/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface exported by (part of) the webapp to allow processes it creates to
 * push in usage records.
 * 
 * @author Donal Fellows
 */
public interface UsageRecordReceiver extends Remote {
	/**
	 * Called to push in a usage record. Note that it is assumed that the usage
	 * record already contains all the information required to locate and
	 * process the job; there is no separate handle.
	 * 
	 * @param usageRecord
	 *            The serialised XML of the usage record.
	 * @throws RemoteException
	 *             if anything goes wrong.
	 */
	void acceptUsageRecord(String usageRecord) throws RemoteException;
}
