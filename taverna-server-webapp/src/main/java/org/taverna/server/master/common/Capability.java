package org.taverna.server.master.common;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * Describes a single capability supported by Taverna Server's workflow
 * execution core.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "Capability")
public class Capability {
	@XmlAttribute
	@XmlSchemaType(name = "anyURI")
	public URI capability;
	@XmlAttribute
	public String version;
}
