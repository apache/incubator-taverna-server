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

import static java.lang.Math.min;
import static java.lang.System.arraycopy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

import org.apache.taverna.server.master.exceptions.FilesystemAccessException;
import org.apache.taverna.server.master.interfaces.File;

/**
 * An MTOM-capable description of how to transfer the contents of a file.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "FileContents")
public class FileContents {
	@XmlElement
	public String name;
	@XmlMimeType("application/octet-stream") // JAXB bug: must be this
	public DataHandler fileData;

	/**
	 * Initialize the contents of this descriptor from the given file and
	 * content type.
	 * 
	 * @param file
	 *            The file that is to be reported.
	 * @param contentType
	 *            The estimated content type of the file.
	 */
	public void setFile(File file, String contentType) {
		name = file.getFullName();
		fileData = new DataHandler(new TavernaFileSource(file, contentType));
	}

	/**
	 * Write the content described by this class to the specified file.
	 * @param file The file to write to; must already exist.
	 * @throws IOException
	 * @throws FilesystemAccessException
	 */
	public void writeToFile(File file) throws IOException,
			FilesystemAccessException {
		try (InputStream is = fileData.getInputStream()) {
			byte[] buf = new byte[65536];
			file.setContents(new byte[0]);
			while (true) {
				int len = is.read(buf);
				if (len <= 0)
					return;
				if (len == buf.length)
					file.appendContents(buf);
				else {
					byte[] shortbuf = new byte[len];
					arraycopy(buf, 0, shortbuf, 0, len);
					file.appendContents(shortbuf);
				}
			}
		}
	}
}

/**
 * A data source that knows how to communicate with the Taverna Server back-end.
 * 
 * @author Donal Fellows
 */
class TavernaFileSource implements DataSource {
	TavernaFileSource(File f, String type) {
		this.f = f;
		this.type = type;
	}

	private final File f;
	private final String type;

	@Override
	public String getContentType() {
		return type;
	}

	@Override
	public String getName() {
		return f.getName();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		final File f = this.f;
		return new InputStream() {
			private int idx;

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				byte[] r;
				try {
					r = f.getContents(idx, len);
				} catch (FilesystemAccessException e) {
					throw new IOException(e);
				}
				if (r == null)
					return -1;
				len = min(len, r.length);
				arraycopy(r, 0, b, off, len);
				idx += len;
				return len;
			}

			@Override
			public int read() throws IOException {
				byte[] r;
				try {
					r = f.getContents(idx, 1);
				} catch (FilesystemAccessException e) {
					throw new IOException(e);
				}
				if (r == null)
					return -1;
				idx++;
				return r[0];
			}
		};
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		final File f = this.f;
		return new OutputStream() {
			private boolean append = false;

			@Override
			public void write(int b) throws IOException {
				write(new byte[] { (byte) b });
			}

			@Override
			public void write(byte[] b) throws IOException {
				try {
					if (append)
						f.appendContents(b);
					else
						f.setContents(b);
					append = true;
				} catch (FilesystemAccessException e) {
					throw new IOException(e);
				}
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				byte[] ary = new byte[len];
				arraycopy(b, off, ary, 0, len);
				write(ary);
			}
		};
	}
}
