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

import org.apache.taverna.server.master.exceptions.FilesystemAccessException;

/**
 * Represents a file in the working directory of a workflow instance run, or in
 * some sub-directory of it.
 * 
 * @author Donal Fellows
 * @see Directory
 */
public interface File extends DirectoryEntry {
	/**
	 * @param offset
	 *            Where in the file to start reading.
	 * @param length
	 *            The length of file to read, or -1 to read to the end of the
	 *            file.
	 * @return The literal byte contents of the section of the file, or null if
	 *         the section doesn't exist.
	 * @throws FilesystemAccessException
	 *             If the read of the file goes wrong.
	 */
	public byte[] getContents(int offset, int length)
			throws FilesystemAccessException;

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
	 * Append the data to the file.
	 * 
	 * @param data
	 *            The literal bytes that will be added on to the end of the
	 *            file.
	 * @throws FilesystemAccessException
	 *             If the write to the file goes wrong.
	 */
	public void appendContents(byte[] data) throws FilesystemAccessException;

	/**
	 * @return The length of the file, in bytes.
	 * @throws FilesystemAccessException
	 *             If the read of the file size goes wrong.
	 */
	public long getSize() throws FilesystemAccessException;

	/**
	 * Asks for the argument file to be copied to this one.
	 * 
	 * @param from
	 *            The source file.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	public void copy(File from) throws FilesystemAccessException;
}
