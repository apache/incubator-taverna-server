/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "workflowOutputs")
@XmlType(propOrder = {})
public class OutputDescription {
	@XmlAttribute
	public String workflowId;
	@XmlAttribute
	@XmlSchemaType(name = "anyURI")
	public URI workflowRun;

	@XmlElement(name = "output")
	public List<Port> ports = new ArrayList<Port>();

	@XmlType
	public static class Port {
		@XmlAttribute(required = true)
		public String name;
		@XmlAttribute
		public Integer depth;
		@XmlElements({
				@XmlElement(name = "value", type = LeafValue.class, nillable = false, required = true),
				@XmlElement(name = "list", type = ListValue.class, nillable = false, required = true),
				@XmlElement(name = "error", type = ErrorValue.class, nillable = false, required = true),
				@XmlElement(name = "absent", type = AbsentValue.class, nillable = false, required = true) })
		public AbstractValue output;
	}
}
