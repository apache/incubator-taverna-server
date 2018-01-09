/*
 */
package org.apache.taverna.server.port_description;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	public List<OutputPort> ports = new ArrayList<>();

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
