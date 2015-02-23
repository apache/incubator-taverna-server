/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Description of what sort of event listener to create and attach to a workflow
 * run. Bound via JAXB.
 * 
 * @author Donal Fellows
 */
@XmlRootElement(name = "listenerDefinition")
@XmlType(name="ListenerDefinition")
public class ListenerDefinition {
	/**
	 * The type of event listener to create.
	 */
	@XmlAttribute
	public String type;
	/**
	 * How the event listener should be configured.
	 */
	@XmlValue
	public String configuration;
}
