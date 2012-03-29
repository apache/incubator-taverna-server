/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.common;

import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static org.apache.commons.logging.LogFactory.getLog;

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
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;

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
	static {
		try {
			JAXBContext context = JAXBContext.newInstance(Workflow.class);
			marshaller = context.createMarshaller();
			unmarshaller = context.createUnmarshaller();
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
			Reader r = new InputStreamReader(new InflaterInputStream(
					new ByteArrayInputStream(bytes)), "UTF-8");
			Workflow w = (Workflow) unmarshaller.unmarshal(r);
			r.close();
			this.content = w.content;
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
			OutputStreamWriter w = new OutputStreamWriter(
					new DeflaterOutputStream(baos), "UTF-8");
			marshaller.marshal(this, w);
			w.close();
			byte[] bytes = baos.toByteArray();
			out.writeInt(bytes.length);
			out.write(bytes);
		} catch (JAXBException e) {
			throw new IOException("failed to marshal", e);
		}
	}
}
