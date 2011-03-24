/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.taverna.server.localworker.server.UsageRecordReceiver;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The main RMI-enabled interface for creating runs.
 * 
 * @author Donal Fellows
 */
public interface RemoteRunFactory extends Remote {
	/**
	 * Makes a workflow run that will process a particular workflow document.
	 * 
	 * @param workflow
	 *            The (serialised) workflow to instantiate as a run.
	 * @param creator
	 *            Who is this run created for?
	 * @param usageRecordReceiver
	 *            Where to write any usage records. May be <tt>null</tt> to
	 *            cause them to not be written.
	 * @return A remote handle for the run.
	 */
	@NonNull
	public RemoteSingleRun make(@NonNull String workflow,
			@NonNull String creator,
			@Nullable UsageRecordReceiver usageRecordReceiver)
			throws RemoteException;

	/**
	 * Asks this factory to unregister itself from the registry and cease
	 * operation.
	 */
	public void shutdown() throws RemoteException;
}
