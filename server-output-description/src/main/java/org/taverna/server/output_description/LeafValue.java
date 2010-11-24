package org.taverna.server.output_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class LeafValue extends AbstractValue {
	@XmlAttribute
	String contentType;
}
