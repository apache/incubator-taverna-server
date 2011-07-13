/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.output_description;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ListValue extends AbstractValue {
	@XmlElements( {
		@XmlElement(name = "value", type = LeafValue.class, nillable = false),
		@XmlElement(name = "list", type = ListValue.class, nillable = false),
		@XmlElement(name = "error", type = ErrorValue.class, nillable = false),
		@XmlElement(name = "absent", type = AbsentValue.class, nillable = false)})
	public List<AbstractValue> contents = new ArrayList<AbstractValue>();
}
