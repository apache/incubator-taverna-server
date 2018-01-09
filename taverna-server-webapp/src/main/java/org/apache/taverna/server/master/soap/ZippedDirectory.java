/*
 */
package org.apache.taverna.server.master.soap;
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

import static org.apache.taverna.server.master.api.ContentTypes.APPLICATION_ZIP_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.UnsupportedDataTypeException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

import org.apache.taverna.server.master.exceptions.FilesystemAccessException;
import org.apache.taverna.server.master.interfaces.Directory;

/**
 * An MTOM-capable description of how to transfer the zipped contents of a
 * directory.
 * 
 * @author Donal Fellows
 * @see Directory#getContentsAsZip()
 */
@XmlType(name = "ZippedDirectory")
public class ZippedDirectory {
	@XmlElement
	public String name;
	@XmlMimeType("application/octet-stream")
	// JAXB bug: must be this
	public DataHandler fileData;

	public ZippedDirectory() {
	}

	/**
	 * Initialise the contents of this descriptor from the given directory.
	 * 
	 * @param dir
	 *            The directory that is to be reported.
	 */
	public ZippedDirectory(Directory dir) {
		name = dir.getFullName();
		fileData = new DataHandler(new ZipSource(dir));
	}
}

/**
 * A data source that knows how to communicate with the Taverna Server back-end.
 * 
 * @author Donal Fellows
 */
class ZipSource implements DataSource {
	ZipSource(Directory d) {
		this.d = d;
	}

	private final Directory d;

	@Override
	public String getContentType() {
		return APPLICATION_ZIP_TYPE.toString();
	}

	@Override
	public String getName() {
		return d.getName();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		try {
			return d.getContentsAsZip();
		} catch (FilesystemAccessException e) {
			throw new IOException(e);
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedDataTypeException();
	}
}
