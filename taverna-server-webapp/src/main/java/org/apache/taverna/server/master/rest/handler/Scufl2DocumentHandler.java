/*
 */
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.common.Workflow;

import org.apache.taverna.scufl2.api.io.ReaderException;
import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.apache.taverna.scufl2.api.io.WriterException;

/**
 * Handler that allows a .scufl2 document to be read from and written to a REST
 * message directly.
 * 
 * @author Donal Fellows
 */
@Provider
public class Scufl2DocumentHandler implements MessageBodyReader<Workflow>,
		MessageBodyWriter<Workflow> {
	private static final MediaType SCUFL2_TYPE = new MediaType("application",
			"vnd.taverna.scufl2.workflow-bundle");
	public static final String SCUFL2 = "application/vnd.taverna.scufl2.workflow-bundle";
	private WorkflowBundleIO io = new WorkflowBundleIO();

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (type.isAssignableFrom(Workflow.class))
			return mediaType.isCompatible(SCUFL2_TYPE);
		return false;
	}

	@Override
	public Workflow readFrom(Class<Workflow> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		try {
			return new Workflow(io.readBundle(entityStream, SCUFL2));
		} catch (ReaderException e) {
			throw new WebApplicationException(e, 403);
		}
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (Workflow.class.isAssignableFrom(type))
			return mediaType.isCompatible(SCUFL2_TYPE);
		return false;
	}

	@Override
	public long getSize(Workflow workflow, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(Workflow workflow, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders,
			OutputStream entityStream) throws IOException,
			WebApplicationException {
		try {
			io.writeBundle(workflow.getScufl2Workflow(), entityStream, SCUFL2);
		} catch (WriterException e) {
			throw new WebApplicationException(e);
		}
	}
}
