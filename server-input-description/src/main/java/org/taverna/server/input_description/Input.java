package org.taverna.server.input_description;

import static org.taverna.server.input_description.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Input {
	@XmlAttribute(namespace = XLINK)
	public URI href;
	@XmlElement(required = true)
	public String name;
	@XmlElement
	public Integer depth;
}
