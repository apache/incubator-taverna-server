/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
@XmlSchema(namespace = DATA, elementFormDefault = QUALIFIED, attributeFormDefault = QUALIFIED, xmlns = {
		@XmlNs(prefix = "port", namespaceURI = DATA),
		@XmlNs(prefix = "xlink", namespaceURI = XLINK),
		@XmlNs(prefix = "rdf", namespaceURI = RDF),
		@XmlNs(prefix = "run", namespaceURI = RUN) })
package org.taverna.server.port_description;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static org.taverna.server.port_description.Namespaces.DATA;
import static org.taverna.server.port_description.Namespaces.RDF;
import static org.taverna.server.port_description.Namespaces.RUN;
import static org.taverna.server.port_description.Namespaces.XLINK;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

