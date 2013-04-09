/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static java.lang.Math.min;

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
import org.taverna.server.master.rest.FileSegment;

/**
 * How to write out a segment of a file with JAX-RS.
 * 
 * @author Donal Fellows
 */
@Provider
public class FileSegmentHandler implements MessageBodyWriter<FileSegment> {
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
		return FileSegment.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(FileSegment t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return t.to - t.from;
	}

	@Override
	public void writeTo(FileSegment t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		try {
			int off = t.from;
			while (off < t.to) {
				byte[] buffer = t.file.getContents(off,
						min(maxChunkSize, t.to - off));
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
