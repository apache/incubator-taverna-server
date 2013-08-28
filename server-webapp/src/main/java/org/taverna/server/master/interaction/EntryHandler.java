package org.taverna.server.master.interaction;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.writer.Writer;
import org.springframework.beans.factory.annotation.Required;

@Provider
@Produces({ "application/atom+xml", "application/atom+xml;type=entry" })
@Consumes({ "application/atom+xml", "application/atom+xml;type=entry" })
public class EntryHandler implements MessageBodyWriter<Entry>, MessageBodyReader<Entry> {
	private static final MediaType ENTRY = new MediaType("application",
			"atom+xml", singletonMap("type", "entry"));

	@Required
	public void setAbdera(Abdera abdera) {
		parser = abdera.getParser();
		writer = abdera.getWriterFactory().getWriter("prettyxml");
	}

	private Parser parser;
	private Writer writer;

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!Entry.class.isAssignableFrom(type))
			return false;
		if (!ENTRY.isCompatible(mediaType))
			return false;
		if (mediaType.getParameters().containsKey("type"))
			return "entry".equalsIgnoreCase(mediaType.getParameters()
					.get("type"));
		return true;
	}

	@Override
	public Entry readFrom(Class<Entry> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		Document<Entry> doc = parser.parse(entityStream);
		if (!Entry.class.isAssignableFrom(doc.getRoot().getClass())) {
			throw new WebApplicationException(Response
					.notAcceptable(asList(new Variant(ENTRY, null, null)))
					.entity("not really a feed entry").build());
		}
		return doc.getRoot();
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!Entry.class.isAssignableFrom(type))
			return false;
		if (!ENTRY.isCompatible(mediaType))
			return false;
		if (mediaType.getParameters().containsKey("type"))
			return "entry".equalsIgnoreCase(mediaType.getParameters()
					.get("type"));
		return true;
	}

	@Override
	public long getSize(Entry t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(Entry t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		httpHeaders.putSingle("Content-Type", ENTRY.toString());
		writer.writeTo(t, entityStream);
	}
}
