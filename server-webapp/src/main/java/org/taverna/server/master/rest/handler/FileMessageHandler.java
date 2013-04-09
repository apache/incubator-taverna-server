/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static org.taverna.server.master.TavernaServerImpl.log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.interfaces.File;

/**
 * How to write out a File object with JAX-RS.
 * 
 * @author Donal Fellows
 */
@Provider
public class FileMessageHandler implements MessageBodyWriter<File> {
	/** How much to pull from the worker in one read. */
	private int maxChunkSize;

	/**
	 * @param maxChunkSize
	 *            How much to pull from the worker in one read.
	 */
	public void setMaxChunkSize(int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return File.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(File t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		try {
			return t.getSize(); // Is it really raw bytes?
		} catch (FilesystemAccessException e) {
			log.info("failed to get file length", e);
			return -1;
		}
	}

	@Override
	public void writeTo(File t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		try {
			int off = 0;
			while (true) {
				byte[] buffer = t.getContents(off, maxChunkSize);
				if (buffer == null || buffer.length == 0)
					break;
				entityStream.write(buffer);
				off += buffer.length;
			}
		} catch (FilesystemAccessException e) {
			throw new IOException("problem when reading file", e);
		}
	}
}
