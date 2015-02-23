/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.annotation.Nonnull;

/**
 * An event listener that is attached to a {@link RemoteSingleRun}.
 * 
 * @author Donal Fellows
 */
public interface RemoteListener extends Remote {
	/**
	 * @return The name of the listener.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String getName() throws RemoteException;

	/**
	 * @return The type of the listener.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String getType() throws RemoteException;

	/**
	 * @return The configuration document for the listener.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String getConfiguration() throws RemoteException;

	/**
	 * @return The supported properties of the listener.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String[] listProperties() throws RemoteException;

	/**
	 * Get the value of a particular property, which should be listed in the
	 * {@link #listProperties()} method.
	 * 
	 * @param propName
	 *            The name of the property to read.
	 * @return The value of the property.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@Nonnull
	public String getProperty(@Nonnull String propName) throws RemoteException;

	/**
	 * Set the value of a particular property, which should be listed in the
	 * {@link #listProperties()} method.
	 * 
	 * @param propName
	 *            The name of the property to write.
	 * @param value
	 *            The value to set the property to.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	public void setProperty(@Nonnull String propName, @Nonnull String value)
			throws RemoteException;
}
