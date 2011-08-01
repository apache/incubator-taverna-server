/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import static org.taverna.server.port_description.Namespaces.RDF;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;

@XmlRootElement
public class InputDescription {
	@XmlAttribute
	public String workflowId;
	@XmlAttribute
	@XmlSchemaType(name = "anyURI")
	public URI workflowRun;
	@XmlAttribute(namespace = RDF)
	public String about;
	@XmlElement
	public List<InputPort> input;
}
