package org.taverna.server.output_description;

import static org.taverna.server.output_description.Namespaces.XLINK;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlSeeAlso( { ErrorValue.class, LeafValue.class, ListValue.class })
public abstract class AbstractValue {
	@XmlAttribute
	String output;
	@XmlAttribute(namespace = XLINK)
	String href;
}
