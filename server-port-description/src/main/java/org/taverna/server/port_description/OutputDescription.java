/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
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
	@XmlElement(name = "output")
	public List<OutputPort> ports = new ArrayList<OutputPort>();

	@XmlType
	public static class OutputPort extends AbstractPort {
		@XmlElements({
				@XmlElement(name = "value", type = LeafValue.class, nillable = false, required = true),
				@XmlElement(name = "list", type = ListValue.class, nillable = false, required = true),
				@XmlElement(name = "error", type = ErrorValue.class, nillable = false, required = true),
				@XmlElement(name = "absent", type = AbsentValue.class, nillable = false, required = true) })
		public AbstractValue output;
	}
}
