/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

import java.io.IOException;
import java.rmi.RemoteException;

import edu.umd.cs.findbugs.annotations.NonNull;

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
	@NonNull
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
	void setContents(@NonNull byte[] data) throws RemoteException, IOException;

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
	void appendContents(@NonNull byte[] data) throws RemoteException,
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
	void copy(@NonNull RemoteFile sourceFile) throws RemoteException,
			IOException;

	/**
	 * @return The full native OS name for the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@NonNull
	String getNativeName() throws RemoteException;

	/**
	 * @return The host holding the file.
	 * @throws RemoteException
	 *             If anything goes wrong with the communication.
	 */
	@NonNull
	String getNativeHost() throws RemoteException;
}
