/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.apache.commons.logging.LogFactory.getLog;

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

import org.apache.commons.logging.Log;

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

/**
 * The actual transfer thunk.
 *
 * @author Donal Fellows
 */
class TransferStream extends InputStream {
	private static final Log log = getLog("Taverna.Server.Handlers");

	public TransferStream(InputStream entityStream, List<String> limit) {
		this.entityStream = new BufferedInputStream(entityStream);
		this.limit = (limit != null && limit.size() > 0) ? parseLong(limit
				.get(0)) : -1;
		if (this.limit == -1)
			log.debug("will attempt to transfer until EOF");
		else
			log.debug("will attempt to transfer " + this.limit + " bytes");
	}

	InputStream entityStream;
	long limit;
	long doneBytes = 0;

	@Override
	public int read() throws IOException {
		if (limit >= 0 && doneBytes >= limit)
			return -1;
		int result = entityStream.read();
		if (result >= 0) {
			doneBytes++;
			log.debug("transferred single byte (" + doneBytes + " total)");
		} else
			log.debug("failed to transfer a single byte; EOF?");
		return result;
	}

	@Override
	public int read(byte[] ary, int off, int len) throws IOException {
		int realLen = len;
		if (limit >= 0) {
			if (doneBytes >= limit)
				return -1;
			if (doneBytes + len > limit)
				len = (int) (limit - doneBytes);
		}
		int readBytes = entityStream.read(ary, off, len);
		if (readBytes >= 0) {
			doneBytes += readBytes;
			log.debug("transferred " + readBytes + " bytes (" + doneBytes
					+ " total, " + realLen + " requested)");
		} else
			log.debug("failed to transfer " + realLen + " bytes; EOF?");
		return readBytes;
	}

	@Override
	public void close() throws IOException {
		entityStream.close();
	}
}