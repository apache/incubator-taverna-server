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
	private Log log = getLog("Taverna.Server.Handlers");

	public TransferStream(InputStream entityStream, List<String> contentLength) {
		this.entityStream = new BufferedInputStream(entityStream);
		if (contentLength != null && contentLength.size() > 0) {
			this.limit = parseLong(contentLength.get(0));
			if (log.isDebugEnabled())
				log.debug("will attempt to transfer " + this.limit + " bytes");
		} else {
			this.limit = -1;
			if (log.isDebugEnabled())
				log.debug("will attempt to transfer until EOF");
		}
	}

	InputStream entityStream;
	long limit;
	long doneBytes = 0;

	@Override
	public int read() throws IOException {
		if (limit >= 0 && doneBytes >= limit)
			return -1;
		int result = entityStream.read();
		if (result >= 0)
			doneBytes++;
		return result;
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
		if (readBytes >= 0)
			doneBytes += readBytes;
		return readBytes;
	}

	@Override
	public void close() throws IOException {
		entityStream.close();
	}
}