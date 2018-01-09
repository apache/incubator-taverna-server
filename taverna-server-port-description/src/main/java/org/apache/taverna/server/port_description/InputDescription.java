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

import static org.apache.taverna.server.port_description.Namespaces.XLINK;

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
	public List<InputPort> input = new ArrayList<>();

	@XmlType(name = "InputPort")
	public static class InputPort extends AbstractPort {
		@XmlAttribute(namespace = XLINK)
		@XmlSchemaType(name = "anyURI")
		public URI href;
	}

	/**
	 * Add an input port to the list of ports.
	 * 
	 * @param name
	 *            The name of the port to add.
	 * @return The port (so that its details may be set);
	 */
	public InputPort addPort(String name) {
		InputPort p = new InputPort();
		p.name = name;
		input.add(p);
		return p;
	}
}
