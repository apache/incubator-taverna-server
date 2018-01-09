/*
 */
package org.taverna.server.localworker.remote;
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

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An entry in a {@link RemoteDirectory} representing a file or sub-directory.
 * 
 * @author Donal Fellows
 * @see RemoteDirectory
 * @see RemoteFile
 */
public interface RemoteDirectoryEntry extends Remote {
	/**
	 * @return The "local" name of the entry. This will never be "<tt>..</tt>"
	 *         or contain the character "<tt>/</tt>".
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String getName() throws RemoteException;

	/**
	 * @return The time when the entry was last modified.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public Date getModificationDate() throws RemoteException;

	/**
	 * Gets the directory containing this directory entry.
	 * 
	 * @return A directory handle, or <tt>null</tt> if called on the workflow
	 *         run's working directory.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nullable
	public RemoteDirectory getContainingDirectory() throws RemoteException;

	/**
	 * Destroy this directory entry, deleting the file or sub-directory. The
	 * workflow run's working directory can never be manually destroyed.
	 * 
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong when deleting the directory entry.
	 */
	public void destroy() throws RemoteException, IOException;
}
