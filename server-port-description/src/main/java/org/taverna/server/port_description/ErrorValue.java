/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.port_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.taverna.server.port_description.utils.IntAdapter;

@XmlType(name = "ErrorValue")
public class ErrorValue extends AbstractValue {
	@XmlAttribute
	@XmlSchemaType(name = "int")
	@XmlJavaTypeAdapter(IntAdapter.class)
	public Integer depth;
	@XmlAttribute(name = "errorFile")
	public String fileName;
	@XmlAttribute(name = "errorByteLength")
	public Long byteLength;
}
