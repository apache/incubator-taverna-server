/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
@XmlSchema(namespace = DATA, elementFormDefault = QUALIFIED, attributeFormDefault = QUALIFIED, xmlns = {
		@XmlNs(prefix = "xlink", namespaceURI = XLINK),
		@XmlNs(prefix = "out-desc", namespaceURI = DATA) })
package org.taverna.server.output_description;

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static org.taverna.server.output_description.Namespaces.DATA;
import static org.taverna.server.output_description.Namespaces.XLINK;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

