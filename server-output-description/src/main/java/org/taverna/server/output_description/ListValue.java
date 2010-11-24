package org.taverna.server.output_description;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class ListValue extends AbstractValue {
	@XmlElement(nillable = false)
	public List<AbstractValue> contents = new ArrayList<AbstractValue>();
}
