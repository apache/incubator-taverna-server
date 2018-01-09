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

import java.io.IOException;
import java.rmi.RemoteException;

import javax.annotation.Nonnull;

/**
 * Represents a file in the working directory of a workflow instance run, or in
 * some sub-directory of it.
 * 
 * @author Donal Fellows
 * @see RemoteDirectory
 */
public interface RemoteFile extends RemoteDirectoryEntry {
	/**
	 * Read from the file.
	 * 
	 * @param offset
	 *            Where in the file to read the bytes from.
	 * @param length
	 *            How much of the file to read; -1 for "to the end".
	 * @return The literal byte contents of the given section of the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong reading the file.
	 */
	@Nonnull
	byte[] getContents(int offset, int length) throws RemoteException,
			IOException;

	/**
	 * Write the data to the file, totally replacing what was there before.
	 * 
	 * @param data
	 *            The literal bytes that will form the new contents of the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong writing the contents.
	 */
	void setContents(@Nonnull byte[] data) throws RemoteException, IOException;

	/**
	 * Append the data to the file.
	 * 
	 * @param data
	 *            The literal bytes that will be appended.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong writing the contents.
	 */
	void appendContents(@Nonnull byte[] data) throws RemoteException,
			IOException;

	/**
	 * @return The length of the file, in bytes.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	long getSize() throws RemoteException;

	/**
	 * Copy from another file to this one.
	 * 
	 * @param sourceFile
	 *            The other file to copy from.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 * @throws IOException
	 *             If things go wrong during the copy.
	 */
	void copy(@Nonnull RemoteFile sourceFile) throws RemoteException,
			IOException;

	/**
	 * @return The full native OS name for the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	String getNativeName() throws RemoteException;

	/**
	 * @return The host holding the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	String getNativeHost() throws RemoteException;
}
