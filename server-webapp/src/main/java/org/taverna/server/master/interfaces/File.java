package org.taverna.server.master.interfaces;

import org.taverna.server.master.exceptions.FilesystemAccessException;

/**
 * Represents a file in the working directory of a workflow instance run, or
 * in some sub-directory of it.
 * 
 * @author Donal Fellows
 * @see Directory
 */
public interface File extends DirectoryEntry {
	/**
	 * @return The literal byte contents of the file.
	 * @throws FilesystemAccessException
	 *             If the read of the file goes wrong.
	 */
	public byte[] getContents() throws FilesystemAccessException;

	/**
	 * Write the data to the file, totally replacing what was there before.
	 * 
	 * @param data
	 *            The literal bytes that will form the new contents of the file.
	 * @throws FilesystemAccessException
	 *             If the write to the file goes wrong.
	 */
	public void setContents(byte[] data) throws FilesystemAccessException;

	/**
	 * @return The length of the file, in bytes.
	 * @throws FilesystemAccessException
	 *             If the read of the file size goes wrong.
	 */
	public long getSize() throws FilesystemAccessException;
}
