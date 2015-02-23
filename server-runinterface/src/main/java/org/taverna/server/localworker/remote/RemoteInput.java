/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

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
