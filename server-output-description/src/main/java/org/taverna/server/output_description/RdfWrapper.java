package org.taverna.server.output_description;

import static org.taverna.server.output_description.Namespaces.RDF;
import static org.taverna.server.output_description.Namespaces.RUN;
import static org.taverna.server.output_description.Namespaces.XLINK;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "rdf", namespace = RDF)
@XmlType(propOrder = {}, namespace = RDF)
public class RdfWrapper {
	@XmlElement
	public Outputs outputsDescription = new Outputs();
	@XmlElements( {
			@XmlElement(name = "value", type = LeafValue.class, nillable = false),
			@XmlElement(name = "list", type = ListValue.class, nillable = false),
			@XmlElement(name = "error", type = ErrorValue.class, nillable = false), })
	public List<AbstractValue> outputs = new ArrayList<AbstractValue>();
	@XmlElement
	public Run run;

	@XmlRootElement(name = "workflowRun", namespace = RUN)
	@XmlType(namespace = RUN)
	public static class Run {
		@XmlAttribute(namespace = RDF)
		public String about;
		@XmlAttribute(namespace = XLINK)
		public String href;
	}
}
