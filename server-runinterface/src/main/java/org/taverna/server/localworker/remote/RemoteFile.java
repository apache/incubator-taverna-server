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
	 * @throws IOException
	 *             If things go wrong.
	 */
	@NonNull
	byte[] getContents(int offset, int length) throws RemoteException,
			IOException;

	/**
	 * Write the data to the file, totally replacing what was there before.
	 * 
	 * @param data
	 *            The literal bytes that will form the new contents of the file.
	 * @throws IOException
	 *             If things go wrong.
	 */
	void setContents(@NonNull byte[] data) throws RemoteException, IOException;

	/**
	 * Append the data to the file.
	 * 
	 * @param data
	 *            The literal bytes that will be appended.
	 * @throws IOException
	 *             If things go wrong.
	 */
	void appendContents(@NonNull byte[] data) throws RemoteException,
			IOException;

	/**
	 * @return The length of the file, in bytes.
	 */
	long getSize() throws RemoteException;

	/**
	 * Copy from another file to this one.
	 * 
	 * @param sourceFile
	 *            The other file to copy from.
	 * @throws IOException
	 *             If things go wrong.
	 */
	void copy(@NonNull RemoteFile sourceFile) throws RemoteException,
			IOException;

	/**
	 * @return The full native OS name for the file.
	 */
	@NonNull
	String getNativeName() throws RemoteException;

	/**
	 * @return The host holding the file.
	 */
	@NonNull
	String getNativeHost() throws RemoteException;
}
