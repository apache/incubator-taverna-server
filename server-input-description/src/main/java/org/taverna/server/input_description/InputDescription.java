package org.taverna.server.input_description;

import static org.taverna.server.input_description.Namespaces.RDF;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InputDescription {
	@XmlAttribute(namespace = RDF)
	public String about;
	@XmlElement
	public List<Input> input;
}
