/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static org.apache.commons.io.IOUtils.copy;
import static org.taverna.server.master.ContentTypes.APPLICATION_ZIP_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.interfaces.Directory.ZipStream;

/**
 * How to write a ZIP file as the result entity of a request.
 * 
 * @author Donal Fellows
 */
@Provider
@Produces("application/zip")
public class ZipStreamHandler implements MessageBodyWriter<ZipStream> {
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return ZipStream.class.isAssignableFrom(type)
				&& mediaType.equals(APPLICATION_ZIP_TYPE);
	}

	@Override
	public long getSize(ZipStream t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(ZipStream zipStream, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		copy(zipStream, entityStream);
	}
}
