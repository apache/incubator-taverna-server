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

import java.util.Date;

import org.apache.taverna.server.master.exceptions.FilesystemAccessException;

/**
 * An entry in a {@link Directory} representing a file or sub-directory.
 * 
 * @author Donal Fellows
 * @see Directory
 * @see File
 */
public interface DirectoryEntry extends Comparable<DirectoryEntry> {
	/**
	 * @return The "local" name of the entry. This will never be "<tt>..</tt>"
	 *         or contain the character "<tt>/</tt>".
	 */
	public String getName();

	/**
	 * @return The "full" name of the entry. This is computed relative to the
	 *         workflow run's working directory. It may contain the "<tt>/</tt>"
	 *         character.
	 */
	public String getFullName();

	/**
	 * @return The time that the entry was last modified.
	 */
	public Date getModificationDate();

	/**
	 * Destroy this directory entry, deleting the file or sub-directory. The
	 * workflow run's working directory can never be manually destroyed.
	 * 
	 * @throws FilesystemAccessException
	 *             If the destroy fails for some reason.
	 */
	public void destroy() throws FilesystemAccessException;
	// TODO: Permissions (or decide not to do anything about them)
}
