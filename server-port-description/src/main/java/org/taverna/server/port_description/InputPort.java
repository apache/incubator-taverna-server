/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import static org.taverna.server.port_description.Namespaces.RDF;
import static org.taverna.server.port_description.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class InputPort {
	@XmlAttribute(namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI href;
	@XmlAttribute(namespace = RDF)
	public String about;
	@XmlAttribute(required = true)
	public String name;
	@XmlAttribute
	public Integer depth;
}
