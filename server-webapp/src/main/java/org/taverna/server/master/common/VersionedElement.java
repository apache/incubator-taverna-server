package org.taverna.server.master.common;

import static org.taverna.server.master.common.Namespaces.SERVER;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * The type of an element that declares the version of the server that produced
 * it.
 * 
 * @author Donal Fellows
 */
@XmlType(namespace = SERVER)
public abstract class VersionedElement {
	/** What version of server produced this element? */
	@XmlAttribute(namespace = SERVER)
	public String serverVersion;
	static final String VERSION = "2.2b1-SNAPSHOT";

	public VersionedElement() {
	}

	protected VersionedElement(boolean ignored) {
		serverVersion = VERSION;
	}
}
