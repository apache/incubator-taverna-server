/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.port_description;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.taverna.server.port_description.utils.IntAdapter;

@XmlType(name = "ListValue")
public class ListValue extends AbstractValue {
	@XmlAttribute
	@XmlSchemaType(name = "int")
	@XmlJavaTypeAdapter(IntAdapter.class)
	public Integer length;
	@XmlElements({
			@XmlElement(name = "value", type = LeafValue.class, nillable = false),
			@XmlElement(name = "list", type = ListValue.class, nillable = false),
			@XmlElement(name = "error", type = ErrorValue.class, nillable = false),
			@XmlElement(name = "absent", type = AbsentValue.class, nillable = false) })
	public List<AbstractValue> contents = new ArrayList<>();
}
