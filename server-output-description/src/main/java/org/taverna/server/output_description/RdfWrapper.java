/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.output_description;

import static org.taverna.server.output_description.Namespaces.XLINK;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "rdf", namespace = RdfWrapper.RDF)
@XmlType(propOrder = {}, namespace = RdfWrapper.RDF)
class RdfWrapper {
	static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static final String RUN = "http://ns.taverna.org.uk/2010/run/";
	@XmlElement
	public Outputs outputsDescription = new Outputs();
	@XmlElements( {
			@XmlElement(name = "value", type = LeafValue.class, nillable = false),
			@XmlElement(name = "list", type = ListValue.class, nillable = false),
			@XmlElement(name = "error", type = ErrorValue.class, nillable = false),
			@XmlElement(name = "absent", type = AbsentValue.class, nillable = false) })
	public List<AbstractValue> outputs = new ArrayList<AbstractValue>();
	@XmlElement(nillable = false)
	public Run run = new Run();

	@XmlRootElement(name = "workflowRun", namespace = RUN)
	@XmlType(propOrder = {}, namespace = RUN)
	public static class Run {
		@XmlAttribute(namespace = RDF)
		public String about;
		@XmlAttribute(namespace = XLINK)
		@XmlSchemaType(name = "anyURI")
		public URI href;
		@XmlElement(name = "runOf", namespace = RUN, nillable = false)
		public RunID runid = new RunID();

		@XmlType(name = "")
		public static class RunID {
			@XmlAttribute(namespace = RDF, required = true)
			public String resource;
		}
	}
}
