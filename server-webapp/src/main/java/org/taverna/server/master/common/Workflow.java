/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.Namespaces.T2FLOW;

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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Encapsulation of a T2flow document.
 * 
 * @author dkf
 */
@XmlRootElement(name = "workflow")
@XmlType(name = "Workflow")
public class Workflow implements Serializable,Externalizable {
	/**
	 * Literal document.
	 */
	@XmlAnyElement(lax = true)
	public Element[] content;

	private static Marshaller marshaller;
	private static Unmarshaller unmarshaller;
	private final static String ENCODING = "UTF-8"; 
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
	}

	public static Workflow unmarshal(String representation)
			throws JAXBException {
		StringReader sr = new StringReader(representation);
		return (Workflow) unmarshaller.unmarshal(sr);
	}

	public String marshal() throws JAXBException {
		StringWriter sw = new StringWriter();
		marshaller.marshal(this, sw);
		return sw.toString();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		try {
			int len = in.readInt();
			byte[] bytes = new byte[len];
			in.readFully(bytes);
			try (Reader r = new InputStreamReader(new InflaterInputStream(
					new ByteArrayInputStream(bytes)), ENCODING)) {
				this.content = ((Workflow) unmarshaller.unmarshal(r)).content;
			}
			return;
		} catch (JAXBException e) {
			throw new IOException("failed to unmarshal", e);
		} catch (ClassCastException e) {
			throw new IOException("bizarre result of unmarshalling", e);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (OutputStreamWriter w = new OutputStreamWriter(
					new DeflaterOutputStream(baos), ENCODING)) {
				marshaller.marshal(this, w);
			}
			byte[] bytes = baos.toByteArray();
			out.writeInt(bytes.length);
			out.write(bytes);
		} catch (JAXBException e) {
			throw new IOException("failed to marshal", e);
		}
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
		Element el = null;
		for (Element e : content)
			if (e.getNamespaceURI().equals(T2FLOW)
					&& e.getLocalName().equals(name[0])) {
				el = e;
				break;
			}
		boolean skip = true;
		for (String n : name) {
			if (skip) {
				skip = false;
				continue;
			}
			if (el == null)
				return null;
			NodeList nl = el.getElementsByTagNameNS(T2FLOW, n);
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
