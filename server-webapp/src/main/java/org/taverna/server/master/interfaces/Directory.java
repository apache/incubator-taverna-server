/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import java.io.PipedInputStream;
import java.security.Principal;
import java.util.Collection;

import org.taverna.server.master.exceptions.FilesystemAccessException;

/**
 * Represents a directory that is the working directory of a workflow run, or a
 * sub-directory of it.
 * 
 * @author Donal Fellows
 * @see File
 */
public interface Directory extends DirectoryEntry {
	/**
	 * @return A list of the contents of the directory.
	 * @throws FilesystemAccessException
	 *             If things go wrong.
	 */
	Collection<DirectoryEntry> getContents() throws FilesystemAccessException;

	/**
	 * @return A list of the contents of the directory, in guaranteed date
	 *         order.
	 * @throws FilesystemAccessException
	 *             If things go wrong.
	 */
	Collection<DirectoryEntry> getContentsByDate()
			throws FilesystemAccessException;

	/**
	 * @return The contents of the directory (and its sub-directories) as a zip.
	 * @throws FilesystemAccessException
	 *             If things go wrong.
	 */
	ZipStream getContentsAsZip() throws FilesystemAccessException;

	/**
	 * Creates a sub-directory of this directory.
	 * 
	 * @param actor
	 *            Who this is being created by.
	 * @param name
	 *            The name of the sub-directory.
	 * @return A handle to the newly-created directory.
	 * @throws FilesystemAccessException
	 *             If the name is the same as some existing entry in the
	 *             directory, or if something else goes wrong during creation.
	 */
	Directory makeSubdirectory(Principal actor, String name)
			throws FilesystemAccessException;

	/**
	 * Creates an empty file in this directory.
	 * 
	 * @param actor
	 *            Who this is being created by.
	 * @param name
	 *            The name of the file to create.
	 * @return A handle to the newly-created file.
	 * @throws FilesystemAccessException
	 *             If the name is the same as some existing entry in the
	 *             directory, or if something else goes wrong during creation.
	 */
	File makeEmptyFile(Principal actor, String name)
			throws FilesystemAccessException;

	/**
	 * A simple pipe that produces the zipped contents of a directory.
	 * 
	 * @author Donal Fellows
	 */
	public static class ZipStream extends PipedInputStream {
	}
}
