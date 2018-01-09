package org.taverna.server.master.rest.handler;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	private static final String ENC = "UTF-8";

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
		httpHeaders.putSingle("Content-Type", FEED.toString() + ";charset="
				+ ENC);
		writer.writeTo(t, new OutputStreamWriter(entityStream, ENC));
	}
}
