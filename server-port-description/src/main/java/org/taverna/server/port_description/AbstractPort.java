/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Port")
public class AbstractPort {
	@XmlAttribute(required = true)
	public String name;
	@XmlAttribute
	public Integer depth;
}
