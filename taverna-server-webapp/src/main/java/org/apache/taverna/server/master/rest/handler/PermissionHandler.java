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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.taverna.server.master.common.Permission;

/**
 * Handler that allows CXF to send and receive {@linkplain Permission
 * permissions} as plain text directly.
 * 
 * @author Donal Fellows
 */
public class PermissionHandler implements MessageBodyReader<Permission>,
		MessageBodyWriter<Permission> {
	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(Permission.class)
				&& mediaType.isCompatible(TEXT_PLAIN_TYPE);
	}

	@Override
	public long getSize(Permission t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return t.toString().length();
	}

	@Override
	public void writeTo(Permission t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		new OutputStreamWriter(entityStream).write(t.toString());
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(Permission.class)
				&& mediaType.isCompatible(TEXT_PLAIN_TYPE);
	}

	@Override
	public Permission readFrom(Class<Permission> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		char[] cbuf = new char[7];
		int len = new InputStreamReader(entityStream).read(cbuf);
		if (len < 0)
			throw new IllegalArgumentException("no entity supplied");
		return Permission.valueOf(new String(cbuf, 0, len));
	}
}
