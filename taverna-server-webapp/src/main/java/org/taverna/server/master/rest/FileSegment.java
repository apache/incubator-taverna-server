/*
 */
package org.taverna.server.master.rest;
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

import static javax.ws.rs.core.Response.ok;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.interfaces.File;

/**
 * Representation of a segment of a file to be read by JAX-RS.
 * 
 * @author Donal Fellows
 */
public class FileSegment {
	/** The file to read a segment of. */
	public final File file;
	/** The offset of the first byte of the segment to read. */
	public Integer from;
	/** The offset of the first byte after the segment to read. */
	public Integer to;

	/**
	 * Parse the HTTP Range header and determine what exact range of the file to
	 * read.
	 * 
	 * @param f
	 *            The file this refers to
	 * @param range
	 *            The content of the Range header.
	 * @throws FilesystemAccessException
	 *             If we can't determine the length of the file (shouldn't
	 *             happen).
	 */
	public FileSegment(File f, String range) throws FilesystemAccessException {
		file = f;
		Matcher m = Pattern.compile("^\\s*bytes=(\\d*)-(\\d*)\\s*$").matcher(
				range);
		if (m.matches()) {
			if (!m.group(1).isEmpty())
				from = Integer.valueOf(m.group(1));
			if (!m.group(2).isEmpty())
				to = Integer.valueOf(m.group(2)) + 1;
			int size = (int) f.getSize();
			if (from == null) {
				from = size - to;
				to = size;
			} else if (to == null)
				to = size;
			else if (to > size)
				to = size;
		}
	}

	/**
	 * Convert to a response, as per RFC 2616.
	 * 
	 * @param type
	 *            The expected type of the data.
	 * @return A JAX-RS response.
	 */
	public Response toResponse(MediaType type) {
		if (from == null && to == null)
			return ok(file).type(type).build();
		if (from >= to)
			return ok("Requested range not satisfiable").status(416).build();
		return ok(this).status(206).type(type).build();
	}
}