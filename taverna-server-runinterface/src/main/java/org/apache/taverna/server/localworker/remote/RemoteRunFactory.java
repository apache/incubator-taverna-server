/*
 */
package org.apache.taverna.server.localworker.remote;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import org.apache.taverna.server.localworker.server.UsageRecordReceiver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
	@Nonnull
	RemoteSingleRun make(@Nonnull byte[] workflow, @Nonnull String creator,
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
	void setInteractionServiceDetails(@Nonnull String host,
			@Nonnull String port, @Nonnull String webdavPath,
			@Nonnull String feedPath) throws RemoteException;

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
