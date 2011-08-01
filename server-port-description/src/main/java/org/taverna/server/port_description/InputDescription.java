/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import static org.taverna.server.port_description.Namespaces.XLINK;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * A description of the inputs of a workflow run, as they are currently known
 * about.
 * 
 * @author Donal Fellows.
 */
@XmlRootElement
public class InputDescription extends AbstractPortDescription {
	@XmlElement
	public List<InputPort> input = new ArrayList<InputPort>();

	@XmlType
	public static class InputPort extends AbstractPort {
		@XmlAttribute(namespace = XLINK)
		@XmlSchemaType(name = "anyURI")
		public URI href;
	}
}
