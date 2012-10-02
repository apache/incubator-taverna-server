/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.common;

import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;

/**
 * Encapsulation of a T2flow document.
 * 
 * @author dkf
 */
@XmlRootElement(name = "workflow")
@XmlType(name = "Workflow")
public class Workflow implements Serializable, Externalizable {
	/** Literal document, if present. */
	@XmlAnyElement(lax = true)
	private Element[] content;
	/** SCUFL2 bundle, if present. */
	@XmlTransient
	private WorkflowBundle bundle;

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
		content = new Element[1];
		content[0] = element;
	}

	public Workflow(WorkflowBundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * What content type would this workflow "prefer" to be?
	 */
	public ContentType getPreferredContentType() {
		if (bundle != null)
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
			if (bundle != null)
				return bundle;
			return io.readBundle(new ByteArrayInputStream(getAsT2Flow()),
					T2FLOW);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("problem when converting to SCUFL2", e);
		}
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
				return content[0];
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
				return e;
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
			byte[] bytes;
			int len = in.readInt();
			if (len > 0) {
				bytes = new byte[len];
				in.readFully(bytes);
				Reader r = new InputStreamReader(
						new ByteArrayInputStream(bytes), ENCODING);
				Workflow w = (Workflow) unmarshaller.unmarshal(r);
				r.close();
				this.content = w.content;
			}
			len = in.readInt();
			if (len > 0) {
				bytes = new byte[len];
				in.readFully(bytes);
				this.bundle = io.readBundle(new ByteArrayInputStream(bytes),
						SCUFL2);
			}
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
			byte[] data;
			if (content != null)
				data = getAsT2Flow();
			else
				data = new byte[0];
			out.writeInt(data.length);
			if (data.length > 0)
				out.write(data);
			if (bundle != null)
				data = getAsScufl2();
			else
				data = new byte[0];
			out.writeInt(data.length);
			if (data.length > 0)
				out.write(data);
		} catch (JAXBException e) {
			throw new IOException("failed to marshal", e);
		} catch (WriterException e) {
			throw new IOException("failed to marshal", e);
		}
	}
}
