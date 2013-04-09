/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.status;
import static org.taverna.server.master.rest.handler.URIListHandler.URI_LIST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * Deserialization and serialization engine for the <tt>{@value #URI_LIST}</tt>
 * content type.
 * 
 * @author Donal Fellows
 */
@Provider
@Consumes(URI_LIST)
public class URIListHandler implements MessageBodyReader<List<URI>>,
		MessageBodyWriter<List<URI>> {
	/** The content type we handle. */
	public static final String URI_LIST = "text/uri-list";
	private static final MediaType URILIST = new MediaType("text", "uri-list");

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(ArrayList.class)
				&& genericType instanceof ParameterizedType
				&& ((Class<?>) ((ParameterizedType) genericType)
						.getActualTypeArguments()[0])
						.isAssignableFrom(URI.class)
				&& URILIST.isCompatible(mediaType);
	}

	@Override
	public List<URI> readFrom(Class<List<URI>> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		String enc = mediaType.getParameters().get("encoding");
		Charset c = (enc == null) ? Charset.defaultCharset() : Charset
				.forName(enc);
		BufferedReader br = new BufferedReader(new InputStreamReader(
				entityStream, c));
		ArrayList<URI> uris = new ArrayList<URI>();
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#"))
				continue;
			try {
				uris.add(new URI(line));
			} catch (URISyntaxException e) {
				throw new WebApplicationException(e, status(422).entity(
						"ill-formed URI").build());
			}
		}
		return uris;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return List.class.isAssignableFrom(type)
				&& genericType instanceof ParameterizedType
				&& ((ParameterizedType) genericType).getActualTypeArguments()[0] == URI.class
				&& URILIST.isCompatible(mediaType);
	}

	@Override
	public long getSize(List<URI> list, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	private static final String PREFERRED_ENCODING = "UTF-8";

	@Override
	public void writeTo(List<URI> list, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException {
		String encoding = mediaType.getParameters().get("encoding");
		if (encoding == null) {
			encoding = PREFERRED_ENCODING;
			httpHeaders.putSingle("Content-Type", URI_LIST + ";encoding="
					+ encoding);
		}
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				entityStream, encoding));
		for (URI uri : list) {
			w.write(uri.toString());
			w.newLine();
		}
		w.flush();
	}
}