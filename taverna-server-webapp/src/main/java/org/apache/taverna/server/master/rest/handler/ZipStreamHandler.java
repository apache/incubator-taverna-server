/*
 */
package org.apache.taverna.server.master.rest.handler;
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

import static org.apache.commons.io.IOUtils.copy;
import static org.apache.taverna.server.master.api.ContentTypes.APPLICATION_ZIP_TYPE;

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

import org.apache.taverna.server.master.interfaces.Directory.ZipStream;

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
