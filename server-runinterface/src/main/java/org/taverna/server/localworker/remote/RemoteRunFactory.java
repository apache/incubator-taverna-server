/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

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
	 * @param masterID
	 *            The UUID of the run to use, or <tt>null</tt> if the execution
	 *            engine is to manufacture a new one for itself.
	 * @return A remote handle for the run.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@NonNull
	RemoteSingleRun make(@NonNull String workflow, @NonNull String creator,
			@Nullable UsageRecordReceiver usageRecordReceiver,
			@Nullable UUID masterID) throws RemoteException;

	/**
	 * Asks this factory to unregister itself from the registry and cease
	 * operation.
	 * 
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	void shutdown() throws RemoteException;

	/**
	 * Configures the details to use when setting up the workflow run's
	 * connnection to the interaction feed.
	 * 
	 * @param host
	 *            The host where the feed is located.
	 * @param port
	 *            The port where the feed is located.
	 * @param webdavPath
	 *            The path used for pushing web pages into the feed.
	 * @param feedPath
	 *            The path used for reading and writing notifications on the
	 *            feed.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	void setInteractionServiceDetails(@NonNull String host,
			@NonNull String port, @NonNull String webdavPath,
			@NonNull String feedPath) throws RemoteException;

	/**
	 * Gets a count of the number of {@linkplain RemoteSingleRun workflow runs}
	 * that this factor knows about that are in the
	 * {@link RemoteStatus#Operating Operating} state.
	 * 
	 * @return A count of "running" workflow runs.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	int countOperatingRuns() throws RemoteException;
}
