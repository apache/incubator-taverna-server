/*
 */
package org.apache.taverna.server.master.utils;
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

import java.util.List;

import javax.ws.rs.core.PathSegment;

import org.apache.taverna.server.master.common.DirEntryReference;
import org.apache.taverna.server.master.exceptions.FilesystemAccessException;
import org.apache.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.apache.taverna.server.master.interfaces.Directory;
import org.apache.taverna.server.master.interfaces.DirectoryEntry;
import org.apache.taverna.server.master.interfaces.File;
import org.apache.taverna.server.master.interfaces.TavernaRun;

/**
 * Utility functions for getting entries from directories.
 * 
 * @author Donal Fellows
 */
public class FilenameUtils {
	private static final String TYPE_ERROR = "trying to take subdirectory of file";
	private static final String NO_FILE = "no such directory entry";
	private static final String NOT_A_FILE = "not a file";
	private static final String NOT_A_DIR = "not a directory";

	/**
	 * Get a named directory entry from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param name
	 *            The name of the directory entry to look up.
	 * @return The directory entry whose name is equal to the last part of the
	 *         path; an empty path will retrieve the working directory handle
	 *         itself.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable.
	 */
	public DirectoryEntry getDirEntry(TavernaRun run, String name)
			throws FilesystemAccessException, NoDirectoryEntryException {
		Directory dir = run.getWorkingDirectory();
		if (name == null || name.isEmpty())
			return dir;
		DirectoryEntry found = dir;
		boolean mustBeLast = false;

		// Must be nested loops; avoids problems with %-encoded "/" chars
		for (String bit : name.split("/")) {
			if (mustBeLast)
				throw new FilesystemAccessException(TYPE_ERROR);
			found = getEntryFromDir(bit, dir);
			dir = null;
			if (found instanceof Directory) {
				dir = (Directory) found;
				mustBeLast = false;
			} else
				mustBeLast = true;
		}
		return found;
	}

	/**
	 * Get a named directory entry from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param d
	 *            The path segments describing what to look up.
	 * @return The directory entry whose name is equal to the last part of the
	 *         path; an empty path will retrieve the working directory handle
	 *         itself.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable.
	 */
	public DirectoryEntry getDirEntry(TavernaRun run, List<PathSegment> d)
			throws FilesystemAccessException, NoDirectoryEntryException {
		Directory dir = run.getWorkingDirectory();
		if (d == null || d.isEmpty())
			return dir;
		DirectoryEntry found = dir;
		boolean mustBeLast = false;

		// Must be nested loops; avoids problems with %-encoded "/" chars
		for (PathSegment segment : d)
			for (String bit : segment.getPath().split("/")) {
				if (mustBeLast)
					throw new FilesystemAccessException(TYPE_ERROR);
				found = getEntryFromDir(bit, dir);
				dir = null;
				if (found instanceof Directory) {
					dir = (Directory) found;
					mustBeLast = false;
				} else
					mustBeLast = true;
			}
		return found;
	}

	/**
	 * Get a named directory entry from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param d
	 *            The directory reference describing what to look up.
	 * @return The directory entry whose name is equal to the last part of the
	 *         path in the directory reference; an empty path will retrieve the
	 *         working directory handle itself.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 */
	public DirectoryEntry getDirEntry(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException, NoDirectoryEntryException {
		Directory dir = run.getWorkingDirectory();
		if (d == null || d.path == null || d.path.isEmpty())
			return dir;
		DirectoryEntry found = dir;
		boolean mustBeLast = false;

		for (String bit : d.path.split("/")) {
			if (mustBeLast)
				throw new FilesystemAccessException(TYPE_ERROR);
			found = getEntryFromDir(bit, dir);
			dir = null;
			if (found instanceof Directory) {
				dir = (Directory) found;
				mustBeLast = false;
			} else
				mustBeLast = true;
		}
		return found;
	}

	/**
	 * Get a named directory entry from a directory.
	 * 
	 * @param name
	 *            The name of the entry; must be "<tt>/</tt>"-free.
	 * @param dir
	 *            The directory to look in.
	 * @return The directory entry whose name is equal to the given name.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable.
	 */
	private DirectoryEntry getEntryFromDir(String name, Directory dir)
			throws FilesystemAccessException, NoDirectoryEntryException {
		if (dir == null)
			throw new FilesystemAccessException(NO_FILE);
		for (DirectoryEntry entry : dir.getContents())
			if (entry.getName().equals(name))
				return entry;
		throw new NoDirectoryEntryException(NO_FILE);
	}

	/**
	 * Get a named directory from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param d
	 *            The directory reference describing what to look up.
	 * @return The directory whose name is equal to the last part of the path in
	 *         the directory reference; an empty path will retrieve the working
	 *         directory handle itself.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable, or if the
	 *             name doesn't refer to a directory.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 */
	public Directory getDirectory(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException, NoDirectoryEntryException {
		DirectoryEntry dirEntry = getDirEntry(run, d);
		if (dirEntry instanceof Directory)
			return (Directory) dirEntry;
		throw new FilesystemAccessException(NOT_A_DIR);
	}

	/**
	 * Get a named directory from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param name
	 *            The name of the directory to look up.
	 * @return The directory.
	 * @throws FilesystemAccessException
	 *             If the directory isn't specified or isn't readable, or if the
	 *             name doesn't refer to a directory.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 */
	public Directory getDirectory(TavernaRun run, String name)
			throws FilesystemAccessException, NoDirectoryEntryException {
		DirectoryEntry dirEntry = getDirEntry(run, name);
		if (dirEntry instanceof Directory)
			return (Directory) dirEntry;
		throw new FilesystemAccessException(NOT_A_DIR);
	}

	/**
	 * Get a named file from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param d
	 *            The directory reference describing what to look up.
	 * @return The file whose name is equal to the last part of the path in the
	 *         directory reference; an empty path will retrieve the working
	 *         directory handle itself.
	 * @throws FilesystemAccessException
	 *             If the file isn't specified or isn't readable, or if the name
	 *             doesn't refer to a file.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 */
	public File getFile(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException, NoDirectoryEntryException {
		DirectoryEntry dirEntry = getDirEntry(run, d);
		if (dirEntry instanceof File)
			return (File) dirEntry;
		throw new FilesystemAccessException(NOT_A_FILE);
	}

	/**
	 * Get a named file from a workflow run.
	 * 
	 * @param run
	 *            The run whose working directory is to be used as the root of
	 *            the search.
	 * @param name
	 *            The name of the file to look up.
	 * @return The file whose name is equal to the last part of the path in the
	 *         directory reference; an empty path will retrieve the working
	 *         directory handle itself.
	 * @throws FilesystemAccessException
	 *             If the file isn't specified or isn't readable, or if the name
	 *             doesn't refer to a file.
	 * @throws NoDirectoryEntryException
	 *             If there is no such entry.
	 */
	public File getFile(TavernaRun run, String name)
			throws FilesystemAccessException, NoDirectoryEntryException {
		DirectoryEntry dirEntry = getDirEntry(run, name);
		if (dirEntry instanceof File)
			return (File) dirEntry;
		throw new FilesystemAccessException(NOT_A_FILE);
	}
}
