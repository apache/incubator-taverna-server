package org.taverna.server.master.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

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
			MultivaluedMap<String, String> httpHeaders,
			final InputStream entityStream) throws IOException,
			WebApplicationException {
		List<String> cl = httpHeaders.get("Content-Length");
		final long limit = cl != null && cl.size() > 0 ? Long.parseLong(cl
				.get(0)) : -1;
		return new InputStream() {
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
		};
	}
}
