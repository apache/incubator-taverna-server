/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.port_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.taverna.server.port_description.utils.IntAdapter;

@XmlType(name = "Port")
public class AbstractPort {
	@XmlAttribute(required = true)
	public String name;

	@XmlAttribute
	@XmlSchemaType(name = "int")
	@XmlJavaTypeAdapter(IntAdapter.class)
	public Integer depth;
}
