/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static org.taverna.server.master.common.Namespaces.SERVER;
import static org.taverna.server.master.common.Namespaces.XLINK;
import static org.taverna.server.master.common.VersionedElement.VERSION;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * A reference to a single workflow run, described using JAXB.
 * 
 * @author Donal Fellows
 * @see org.taverna.server.master.interfaces.TavernaRun TavernaRun
 */
@XmlRootElement
@XmlType(name = "TavernaRun")
@XmlSeeAlso( { Workflow.class, DirEntryReference.class })
public class RunReference {
	/**
	 * Where to get information about the run. For REST.
	 */
	@XmlAttribute(name = "href", namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI link;
	/** What version of server produced this element? */
	@XmlAttribute(namespace = SERVER)
	public String serverVersion;
	/**
	 * The name of the run. For SOAP.
	 */
	@XmlValue
	public String name;

	/**
	 * Make a blank run reference.
	 */
	public RunReference() {
	}

	/**
	 * Make a reference to the given workflow run.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param ub
	 *            A factory for URIs, or <tt>null</tt> if none is to be made.
	 */
	public RunReference(String name, UriBuilder ub) {
		this.serverVersion = VERSION;
		this.name = name;
		if (ub != null)
			this.link = ub.build(name);
	}
}
