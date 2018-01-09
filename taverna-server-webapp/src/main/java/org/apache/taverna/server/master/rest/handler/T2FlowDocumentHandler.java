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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.taverna.server.master.common.Workflow;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handler that allows a .t2flow document to be read from and written to a REST
 * message directly.
 * 
 * @author Donal Fellows
 */
@Provider
public class T2FlowDocumentHandler implements MessageBodyReader<Workflow>,
		MessageBodyWriter<Workflow> {
	private static final MediaType T2FLOW_TYPE = new MediaType("application",
			"vnd.taverna.t2flow+xml");
	public static final String T2FLOW = "application/vnd.taverna.t2flow+xml";
	public static final String T2FLOW_ROOTNAME = "workflow";
	public static final String T2FLOW_NS = "http://taverna.sf.net/2008/xml/t2flow";
	private DocumentBuilderFactory db;
	private TransformerFactory transformer;

	public T2FlowDocumentHandler() throws ParserConfigurationException,
			TransformerConfigurationException {
		db = DocumentBuilderFactory.newInstance();
		db.setNamespaceAware(true);
		transformer = TransformerFactory.newInstance();
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (type.isAssignableFrom(Workflow.class))
			return mediaType.isCompatible(T2FLOW_TYPE);
		return false;
	}

	@Override
	public Workflow readFrom(Class<Workflow> type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		Document doc;
		try {
			doc = db.newDocumentBuilder().parse(entityStream);
		} catch (SAXException e) {
			throw new WebApplicationException(e, 403);
		} catch (ParserConfigurationException e) {
			throw new WebApplicationException(e);
		}
		Workflow workflow = new Workflow(doc.getDocumentElement());
		if (doc.getDocumentElement().getNamespaceURI().equals(T2FLOW_NS)
				&& doc.getDocumentElement().getNodeName()
						.equals(T2FLOW_ROOTNAME))
			return workflow;
		throw new WebApplicationException(Response.status(403)
				.entity("invalid T2flow document; bad root element")
				.type("text/plain").build());
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		if (Workflow.class.isAssignableFrom(type))
			return mediaType.isCompatible(T2FLOW_TYPE);
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
			transformer.newTransformer().transform(
					new DOMSource(workflow.getT2flowWorkflow()),
					new StreamResult(entityStream));
		} catch (TransformerException e) {
			if (e.getCause() != null && e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
			throw new WebApplicationException(e);
		}
	}
}
