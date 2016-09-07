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
import java.rmi.RemoteException;
import java.util.Collection;

import javax.annotation.Nonnull;

/**
 * Represents a directory that is the working directory of a workflow run, or a
 * sub-directory of it.
 * 
 * @author Donal Fellows
 * @see RemoteFile
 */
public interface RemoteDirectory extends RemoteDirectoryEntry {
	/**
	 * @return A list of the contents of the directory.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If anything goes wrong with listing the directory.
	 */
	@Nonnull
	public Collection<RemoteDirectoryEntry> getContents()
			throws RemoteException, IOException;

	/**
	 * Creates a sub-directory of this directory.
	 * 
	 * @param name
	 *            The name of the sub-directory.
	 * @return A handle to the newly-created directory.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong with creating the subdirectory.
	 */
	@Nonnull
	public RemoteDirectory makeSubdirectory(@Nonnull String name)
			throws RemoteException, IOException;

	/**
	 * Creates an empty file in this directory.
	 * 
	 * @param name
	 *            The name of the file to create.
	 * @return A handle to the newly-created file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If anything goes wrong with creating the file.
	 */
	@Nonnull
	public RemoteFile makeEmptyFile(@Nonnull String name)
			throws RemoteException, IOException;
}
