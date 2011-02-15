package org.taverna.server.input_description;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InputDescription {
	@XmlElement
	List<Input> input;
}
