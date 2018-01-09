/*
 */
package org.apache.taverna.server.master.interfaces;
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

import java.io.PipedInputStream;
import java.security.Principal;
import java.util.Collection;

import org.apache.taverna.server.master.exceptions.FilesystemAccessException;

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
