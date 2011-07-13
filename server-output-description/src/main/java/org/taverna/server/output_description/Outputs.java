/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.output_description;

import static org.taverna.server.output_description.Namespaces.RDF;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "workflowOutputs")
@XmlType(propOrder = {})
public class Outputs {
	@XmlAttribute(namespace = RDF)
	public String about;
	@XmlElement(nillable = false)
	public List<Contains> contains = new ArrayList<Contains>();

	@XmlType
	public static class Contains {
		@XmlAttribute(namespace = RDF)
		public String resource;
	}
}
