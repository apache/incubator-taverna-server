package org.taverna.server.localworker.remote;

import java.io.IOException;
import java.rmi.RemoteException;

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
	public byte[] getContents(int offset, int length) throws RemoteException,
			IOException;

	/**
	 * Write the data to the file, totally replacing what was there before.
	 * 
	 * @param data
	 *            The literal bytes that will form the new contents of the file.
	 * @throws IOException
	 *             If things go wrong.
	 */
	public void setContents(byte[] data) throws RemoteException, IOException;

	/**
	 * Append the data to the file.
	 * 
	 * @param data
	 *            The literal bytes that will be appended.
	 * @throws IOException
	 *             If things go wrong.
	 */
	public void appendContents(byte[] data) throws RemoteException, IOException;

	/**
	 * @return The length of the file, in bytes.
	 */
	public long getSize() throws RemoteException;
}
