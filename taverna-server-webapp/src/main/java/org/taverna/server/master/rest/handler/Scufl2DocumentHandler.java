/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.rest.handler;

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

import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;

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
