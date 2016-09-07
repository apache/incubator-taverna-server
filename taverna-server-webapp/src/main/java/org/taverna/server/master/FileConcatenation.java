package org.taverna.server.master;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.interfaces.File;

/**
 * Simple concatenation of files.
 * 
 * @author Donal Fellows
 */
public class FileConcatenation implements Iterable<File> {
	private List<File> files = new ArrayList<>();

	public void add(File f) {
		files.add(f);
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	/**
	 * @return The total length of the files, or -1 if this cannot be
	 *         determined.
	 */
	public long size() {
		long size = 0;
		for (File f : files)
			try {
				size += f.getSize();
			} catch (FilesystemAccessException e) {
				// Ignore; shouldn't happen but can't guarantee
			}
		return (size == 0 && !files.isEmpty() ? -1 : size);
	}

	/**
	 * Get the concatenated files.
	 * 
	 * @param encoding
	 *            The encoding to use.
	 * @return The concatenated files.
	 * @throws UnsupportedEncodingException
	 *             If the encoding doesn't exist.
	 */
	public String get(String encoding) throws UnsupportedEncodingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (File f : files)
			try {
				baos.write(f.getContents(0, -1));
			} catch (FilesystemAccessException | IOException e) {
				continue;
			}
		return baos.toString(encoding);
	}

	@Override
	public Iterator<File> iterator() {
		return files.iterator();
	}
}