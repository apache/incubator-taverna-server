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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This represents the assignment of inputs to input ports of the workflow. Note
 * that the <tt>file</tt> and <tt>value</tt> properties are never set at the
 * same time.
 * 
 * @author Donal Fellows
 */
public interface RemoteInput extends Remote {
	/**
	 * @return The file currently assigned to this input port, or <tt>null</tt>
	 *         if no file is assigned.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nullable
	String getFile() throws RemoteException;

	/**
	 * @return The name of this input port. This may not be changed.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	String getName() throws RemoteException;

	/**
	 * @return The value currently assigned to this input port, or <tt>null</tt>
	 *         if no value is assigned.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nullable
	String getValue() throws RemoteException;

	/**
	 * @return The delimiter currently used to split this input port's value
	 *         into a list, or <tt>null</tt> if no delimiter is to be used
	 *         (i.e., the value is a singleton).
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nullable
	String getDelimiter() throws RemoteException;

	/**
	 * Sets the file to use for this input. This overrides the use of the
	 * previous file and any set value.
	 * 
	 * @param file
	 *            The filename to use. Must not start with a <tt>/</tt> or
	 *            contain any <tt>..</tt> segments. Will be interpreted relative
	 *            to the run's working directory.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	void setFile(@Nonnull String file) throws RemoteException;

	/**
	 * Sets the value to use for this input. This overrides the use of the
	 * previous value and any set file.
	 * 
	 * @param value
	 *            The value to use.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	void setValue(@Nonnull String value) throws RemoteException;

	/**
	 * Sets the delimiter used to split this input port's value into a list.
	 * 
	 * @param delimiter
	 *            The delimiter character, or <tt>null</tt> if no delimiter is
	 *            to be used (i.e., the value is a singleton).
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	void setDelimiter(@Nullable String delimiter) throws RemoteException;
}
