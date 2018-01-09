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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.FileConcatenation;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.interfaces.File;

public class FileConcatenationHandler implements
		MessageBodyWriter<FileConcatenation> {
	/** How much to pull from the worker in one read. */
	private int maxChunkSize;

	/**
	 * @param maxChunkSize
	 *            How much to pull from the worker in one read.
	 */
	@Required
	public void setMaxChunkSize(int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return type.isAssignableFrom(FileConcatenation.class);
	}

	@Override
	public long getSize(FileConcatenation fc, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return fc.size();
	}

	@Override
	public void writeTo(FileConcatenation fc, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException {
		for (File f : fc)
			try {
				byte[] buffer;
				for (int off = 0; true ; off += buffer.length) {
					buffer = f.getContents(off, maxChunkSize);
					if (buffer == null || buffer.length == 0)
						break;
					entityStream.write(buffer);
				}
			} catch (FilesystemAccessException e) {
				// Ignore/skip to next file
			}
	}
}
