/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.soap;

import static org.taverna.server.master.ContentTypes.APPLICATION_ZIP_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.UnsupportedDataTypeException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.interfaces.Directory;

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
	 * Initialize the contents of this descriptor from the given directory.
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
