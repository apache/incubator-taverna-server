/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.port_description;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A description of the outputs of a workflow run, as they are currently known
 * about.
 * 
 * @author Donal Fellows
 */
@XmlRootElement(name = "workflowOutputs")
public class OutputDescription extends AbstractPortDescription {
	private static final AbsentValue ABSENT_VALUE = new AbsentValue();
	@XmlElement(name = "output")
	public List<OutputPort> ports = new ArrayList<OutputPort>();

	@XmlType(name = "OutputPort")
	public static class OutputPort extends AbstractPort {
		@XmlElements({
				@XmlElement(name = "value", type = LeafValue.class, nillable = false, required = true),
				@XmlElement(name = "list", type = ListValue.class, nillable = false, required = true),
				@XmlElement(name = "error", type = ErrorValue.class, nillable = false, required = true),
				@XmlElement(name = "absent", type = AbsentValue.class, nillable = false, required = true) })
		public AbstractValue output;
	}

	/**
	 * Add an output port to the list of ports.
	 * 
	 * @param name
	 *            The name of the port to add.
	 * @return The port (so that its value may be set);
	 */
	public OutputPort addPort(String name) {
		OutputPort p = new OutputPort();
		p.name = name;
		p.output = ABSENT_VALUE;
		ports.add(p);
		return p;
	}
}
