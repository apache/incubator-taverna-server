/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PortDescription")
public abstract class AbstractPortDescription {
	@XmlAttribute
	public String workflowId;
	@XmlAttribute
	@XmlSchemaType(name = "anyURI")
	public URI workflowRun;
	@XmlAttribute
	public String workflowRunId;

	public void fillInBaseData(String docId, String runId, URI runUrl) {
		this.workflowId = docId;
		this.workflowRun = runUrl;
		this.workflowRunId = runId;
	}
}
