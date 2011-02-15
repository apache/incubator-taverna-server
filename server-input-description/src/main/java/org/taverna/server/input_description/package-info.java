@XmlSchema(namespace = Namespaces.DATA, xmlns = {
		@XmlNs(prefix = "in", namespaceURI = Namespaces.DATA),
		@XmlNs(prefix = "xlink", namespaceURI = Namespaces.XLINK),
		@XmlNs(prefix = "rdf", namespaceURI = Namespaces.RDF),
		@XmlNs(prefix = "run", namespaceURI = Namespaces.RUN) }, elementFormDefault = QUALIFIED, attributeFormDefault = QUALIFIED)
package org.taverna.server.input_description;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

