/*
 */
package org.taverna.server.master.common;
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

import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.annotation.XmlAccessType.NONE;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.rest.handler.Scufl2DocumentHandler.SCUFL2;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW_NS;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW_ROOTNAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.DocumentBuilderFactory;

import org.taverna.server.master.rest.handler.Scufl2DocumentHandler;
import org.taverna.server.master.rest.handler.T2FlowDocumentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.taverna.scufl2.api.common.NamedSet;
import org.apache.taverna.scufl2.api.container.WorkflowBundle;
import org.apache.taverna.scufl2.api.io.ReaderException;
import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.apache.taverna.scufl2.api.io.WriterException;
import org.apache.taverna.scufl2.api.profiles.Profile;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Encapsulation of a T2flow or Scufl2 document.
 * 
 * @author Donal K. Fellows
 */
@XmlRootElement(name = "workflow")
@XmlType(name = "Workflow")
@XmlAccessorType(NONE)
public class Workflow implements Serializable, Externalizable {
	/** Literal document, if present. */
	@XmlAnyElement(lax = true)
	private Element content;
	/** SCUFL2 bundle, if present. */
	@XmlTransient
	private WorkflowBundle bundle;
	/** Which came first, the bundle or the t2flow document. */
	@XmlTransient
	private boolean isBundleFirst;

	private static Marshaller marshaller;
	private static Unmarshaller unmarshaller;
	private final static String ENCODING = "UTF-8";
	private final static WorkflowBundleIO io;
	static {
		try {
			JAXBContext context = JAXBContext.newInstance(Workflow.class);
			marshaller = context.createMarshaller();
			unmarshaller = context.createUnmarshaller();
			marshaller.setProperty(JAXB_ENCODING, ENCODING);
			marshaller.setProperty(JAXB_FORMATTED_OUTPUT, false);
		} catch (JAXBException e) {
			getLog("Taverna.Server.Webapp").fatal(
					"failed to build JAXB context for working with "
							+ Workflow.class, e);
		}
		io = new WorkflowBundleIO();
	}

	public enum ContentType {
		T2FLOW(T2FlowDocumentHandler.T2FLOW), SCUFL2(
				Scufl2DocumentHandler.SCUFL2);
		private String type;

		ContentType(String type) {
			this.type = type;
		}

		public String getContentType() {
			return type;
		}
	}

	public Workflow() {
	}

	public Workflow(Element element) {
		this.content = element;
		this.isBundleFirst = false;
	}

	public Workflow(WorkflowBundle bundle) {
		this.bundle = bundle;
		this.isBundleFirst = true;
	}

	public Workflow(URL url) throws ReaderException, IOException {
		this(io.readBundle(url, null));
	}

	/**
	 * What content type would this workflow "prefer" to be?
	 */
	public ContentType getPreferredContentType() {
		if (isBundleFirst)
			return ContentType.SCUFL2;
		else
			return ContentType.T2FLOW;
	}

	/**
	 * Retrieves the workflow as a SCUFL2 document, converting it if necessary.
	 * 
	 * @return The SCUFL2 document.
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	public WorkflowBundle getScufl2Workflow() throws IOException {
		try {
			if (bundle == null)
				bundle = io.readBundle(new ByteArrayInputStream(getAsT2Flow()),
						T2FLOW);
			return bundle;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("problem when converting to SCUFL2", e);
		}
	}

	/**
	 * Get the bytes of the serialized SCUFL2 workflow.
	 * 
	 * @return Array of bytes.
	 * @throws IOException
	 *             If serialization fails.
	 * @throws WriterException
	 *             If conversion fails.
	 */
	public byte[] getScufl2Bytes() throws IOException, WriterException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		io.writeBundle(getScufl2Workflow(), baos, SCUFL2);
		return baos.toByteArray();
	}

	/**
	 * Retrieves the workflow as a T2Flow document, converting it if necessary.
	 * 
	 * @return The T2Flow document.
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	public Element getT2flowWorkflow() throws IOException {
		try {
			if (content != null)
				return content;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			io.writeBundle(bundle, baos, T2FLOW);
			Document doc;
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				dbf.setNamespaceAware(true);
				doc = dbf.newDocumentBuilder().parse(
						new ByteArrayInputStream(baos.toByteArray()));
			} catch (SAXException e) {
				throw new IOException("failed to convert to DOM tree", e);
			}
			Element e = doc.getDocumentElement();
			if (e.getNamespaceURI().equals(T2FLOW_NS)
					&& e.getNodeName().equals(T2FLOW_ROOTNAME))
				return content = e;
			throw new IOException(
					"unexpected element when converting to T2Flow: {"
							+ e.getNamespaceURI() + "}" + e.getNodeName());
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("problem when converting to SCUFL2", e);
		}
	}

	/**
	 * @return The name of the main workflow profile, or <tt>null</tt> if there
	 *         is none.
	 */
	public String getMainProfileName() {
		try {
			return getScufl2Workflow().getMainProfile().getName();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * @return The set of profiles supported over this workflow.
	 */
	public NamedSet<Profile> getProfiles() {
		try {
			return getScufl2Workflow().getProfiles();
		} catch (IOException e) {
			return new NamedSet<Profile>();
		}
	}

	/**
	 * Convert from marshalled form.
	 * 
	 * @throws JAXBException
	 *             If the conversion fails.
	 */
	public static Workflow unmarshal(String representation)
			throws JAXBException {
		StringReader sr = new StringReader(representation);
		return (Workflow) unmarshaller.unmarshal(sr);
	}

	/**
	 * Convert to marshalled form.
	 */
	public String marshal() throws JAXBException {
		StringWriter sw = new StringWriter();
		marshaller.marshal(this, sw);
		return sw.toString();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		try {
			ByteArrayInputStream bytes = readbytes(in);
			if (bytes != null)
				try (Reader r = new InputStreamReader(bytes, ENCODING)) {
					content = ((Workflow) unmarshaller.unmarshal(r)).content;
				}
			bytes = readbytes(in);
			if (bytes != null)
				bundle = io.readBundle(bytes, SCUFL2);
			isBundleFirst = in.readBoolean();
			return;
		} catch (JAXBException e) {
			throw new IOException("failed to unmarshal", e);
		} catch (ClassCastException e) {
			throw new IOException("bizarre result of unmarshalling", e);
		} catch (ReaderException e) {
			throw new IOException("failed to unmarshal", e);
		}
	}

	private byte[] getAsT2Flow() throws IOException, JAXBException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter w = new OutputStreamWriter(baos, ENCODING);
		marshaller.marshal(this, w);
		w.close();
		return baos.toByteArray();
	}

	private byte[] getAsScufl2() throws IOException, WriterException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		io.writeBundle(bundle, baos, SCUFL2);
		baos.close();
		return baos.toByteArray();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			writebytes(out, (content != null) ? getAsT2Flow() : null);
		} catch (JAXBException e) {
			throw new IOException("failed to marshal t2flow", e);
		}
		try {
			writebytes(out, (bundle != null) ? getAsScufl2() : null);
		} catch (WriterException e) {
			throw new IOException("failed to marshal scufl2", e);
		}
		out.writeBoolean(isBundleFirst);
	}

	private ByteArrayInputStream readbytes(ObjectInput in) throws IOException {
		int len = in.readInt();
		if (len > 0) {
			byte[] bytes = new byte[len];
			in.readFully(bytes);
			return new ByteArrayInputStream(bytes);
		}
		return null;
	}

	private void writebytes(ObjectOutput out, byte[] data) throws IOException {
		out.writeInt(data == null ? 0 : data.length);
		if (data != null && data.length > 0)
			out.write(data);
	}

	/**
	 * Make up for the lack of an integrated XPath engine.
	 * 
	 * @param name
	 *            The element names to look up from the root of the contained
	 *            document.
	 * @return The looked up element, or <tt>null</tt> if it doesn't exist.
	 */
	private Element getEl(String... name) {
		Element el = content;
		boolean skip = true;
		for (String n : name) {
			if (skip) {
				skip = false;
				continue;
			}
			if (el == null)
				return null;
			NodeList nl = el.getElementsByTagNameNS(T2FLOW_NS, n);
			if (nl.getLength() == 0)
				return null;
			Node node = nl.item(0);
			if (node instanceof Element)
				el = (Element) node;
			else
				return null;
		}
		return el;
	}

	/**
	 * @return The content of the embedded
	 *         <tt>&lt;workflow&gt;&lt;dataflow&gt;&lt;name&gt;</tt> element.
	 */
	@XmlTransient
	public String getName() {
		return getEl("workflow", "dataflow", "name").getTextContent();
	}

	/**
	 * @return The embedded <tt>&lt;workflow&gt;</tt> element.
	 */
	@XmlTransient
	public Element getWorkflowRoot() {
		return getEl("workflow");
	}
}
