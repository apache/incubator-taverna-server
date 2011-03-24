package org.taverna.server.localworker.remote;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

import edu.umd.cs.findbugs.annotations.NonNull;

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
	 */
	@NonNull
	public Collection<RemoteDirectoryEntry> getContents()
			throws RemoteException, IOException;

	/**
	 * Creates a sub-directory of this directory.
	 * 
	 * @param name
	 *            The name of the sub-directory.
	 * @return A handle to the newly-created directory.
	 * @throws IOException
	 *             If things go wrong.
	 */
	@NonNull
	public RemoteDirectory makeSubdirectory(@NonNull String name)
			throws RemoteException, IOException;

	/**
	 * Creates an empty file in this directory.
	 * 
	 * @param name
	 *            The name of the file to create.
	 * @return A handle to the newly-created file.
	 */
	@NonNull
	public RemoteFile makeEmptyFile(@NonNull String name)
			throws RemoteException, IOException;
}
