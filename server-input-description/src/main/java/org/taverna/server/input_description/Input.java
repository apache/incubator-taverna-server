/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.input_description;

import static org.taverna.server.input_description.Namespaces.RDF;
import static org.taverna.server.input_description.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlRootElement
public class Input {
	@XmlAttribute(namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI href;
	@XmlAttribute(namespace = RDF)
	public String about;
	@XmlElement(required = true)
	public String name;
	@XmlElement
	public Integer depth;
}
