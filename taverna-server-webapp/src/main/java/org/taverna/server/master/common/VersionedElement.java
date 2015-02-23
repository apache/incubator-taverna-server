/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.Namespaces.SERVER;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.logging.Log;

/**
 * The type of an element that declares the version of the server that produced
 * it.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "VersionedElement", namespace = SERVER)
public abstract class VersionedElement {
	/** What version of server produced this element? */
	@XmlAttribute(namespace = SERVER)
	public String serverVersion;
	/** What revision of server produced this element? Derived from SCM commit. */
	@XmlAttribute(namespace = SERVER)
	public String serverRevision;
	/** When was the server built? */
	@XmlAttribute(namespace = SERVER)
	public String serverBuildTimestamp;
	public static final String VERSION, REVISION, TIMESTAMP;
	static {
		Log log = getLog("Taverna.Server.Webapp");
		Properties p = new Properties();
		try {
			try (InputStream is = VersionedElement.class
					.getResourceAsStream("/version.properties")) {
				p.load(is);
			}
		} catch (IOException e) {
			log.warn("failed to read /version.properties", e);
		}
		VERSION = p.getProperty("tavernaserver.version", "unknownVersion");
		REVISION = String.format("%s (tag: %s)",
				p.getProperty("tavernaserver.branch", "unknownRevision"),
				p.getProperty("tavernaserver.revision.describe", "unknownTag"));
		TIMESTAMP = p
				.getProperty("tavernaserver.timestamp", "unknownTimestamp");
	}

	public VersionedElement() {
	}

	protected VersionedElement(boolean ignored) {
		serverVersion = VERSION;
		serverRevision = REVISION;
		serverBuildTimestamp = TIMESTAMP;
	}
}
