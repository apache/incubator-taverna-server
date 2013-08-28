package org.taverna.server.master.interaction;

import static java.util.Collections.singletonMap;

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

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Feed;
import org.apache.abdera.writer.Writer;
import org.springframework.beans.factory.annotation.Required;

@Provider
@Produces({ "application/atom+xml", "application/atom+xml;type=feed" })
public class FeedHandler implements MessageBodyWriter<Feed> {
	private static final MediaType FEED = new MediaType("application",
			"atom+xml", singletonMap("type", "feed"));

	@Required
	public void setAbdera(Abdera abdera) {
		writer = abdera.getWriterFactory().getWriter("prettyxml");
	}

	private Writer writer;

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (!Feed.class.isAssignableFrom(type))
			return false;
		if (!FEED.isCompatible(mediaType))
			return false;
		if (mediaType.getParameters().containsKey("type"))
			return "feed".equalsIgnoreCase(mediaType.getParameters()
					.get("type"));
		return true;
	}

	@Override
	public long getSize(Feed t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(Feed t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		httpHeaders.putSingle("Content-Type", FEED.toString());
		writer.writeTo(t, entityStream);
	}
}
