@XmlSchema(namespace = DATA, elementFormDefault = QUALIFIED, attributeFormDefault = QUALIFIED, xmlns = {
		@XmlNs(prefix = "in-desc", namespaceURI = DATA),
		@XmlNs(prefix = "xlink", namespaceURI = XLINK),
		@XmlNs(prefix = "rdf", namespaceURI = RDF),
		@XmlNs(prefix = "run", namespaceURI = RUN) })
package org.taverna.server.input_description;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static org.taverna.server.input_description.Namespaces.DATA;
import static org.taverna.server.input_description.Namespaces.RDF;
import static org.taverna.server.input_description.Namespaces.RUN;
import static org.taverna.server.input_description.Namespaces.XLINK;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

