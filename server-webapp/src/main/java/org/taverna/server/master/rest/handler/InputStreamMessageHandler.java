/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

/**
 * Maps a stream from a client into a bounded ordinary input stream that the
 * webapp can work with more easily.
 * 
 * @author Donal Fellows
 */
@Provider
@Consumes(APPLICATION_OCTET_STREAM)
public class InputStreamMessageHandler implements
		MessageBodyReader<InputStream> {
	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return InputStream.class.isAssignableFrom(type);
	}

	@Override
	public InputStream readFrom(Class<InputStream> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		return new TransferStream(entityStream,
				httpHeaders.get("Content-Length"));
	}
}

class TransferStream extends InputStream {
	public TransferStream(InputStream entityStream, List<String> limit) {
		this.entityStream = new BufferedInputStream(entityStream);
		this.limit = limit != null && limit.size() > 0 ? Long.parseLong(limit
				.get(0)) : -1;
	}

	InputStream entityStream;
	long limit;
	long doneBytes = 0;

	@Override
	public int read() throws IOException {
		doneBytes++;
		if (limit >= 0 && doneBytes >= limit)
			return -1;
		return entityStream.read();
	}

	@Override
	public int read(byte[] ary, int off, int len) throws IOException {
		if (limit >= 0) {
			if (doneBytes >= limit)
				return -1;
			if (doneBytes + len > limit)
				len = (int) (limit - doneBytes);
		}
		int readBytes = entityStream.read(ary, off, len);
		if (readBytes > 0)
			doneBytes += readBytes;
		return readBytes;
	}
}