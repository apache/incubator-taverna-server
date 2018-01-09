/*
 */
package org.apache.taverna.server.master.common;
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

import static org.apache.taverna.server.master.common.Namespaces.SERVER;
import static org.apache.taverna.server.master.common.Namespaces.XLINK;
import static org.apache.taverna.server.master.common.VersionedElement.VERSION;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * A reference to a single workflow run, described using JAXB.
 * 
 * @author Donal Fellows
 * @see org.apache.taverna.server.master.interfaces.TavernaRun TavernaRun
 */
@XmlRootElement
@XmlType(name = "TavernaRun")
@XmlSeeAlso( { Workflow.class, DirEntryReference.class })
public class RunReference {
	/**
	 * Where to get information about the run. For REST.
	 */
	@XmlAttribute(name = "href", namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI link;
	/** What version of server produced this element? */
	@XmlAttribute(namespace = SERVER)
	public String serverVersion;
	/**
	 * The name of the run. For SOAP.
	 */
	@XmlValue
	public String name;

	/**
	 * Make a blank run reference.
	 */
	public RunReference() {
	}

	/**
	 * Make a reference to the given workflow run.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param ub
	 *            A factory for URIs, or <tt>null</tt> if none is to be made.
	 */
	public RunReference(String name, UriBuilder ub) {
		this.serverVersion = VERSION;
		this.name = name;
		if (ub != null)
			this.link = ub.build(name);
	}
}
