/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.output_description;

import static org.taverna.server.output_description.Namespaces.RDF;
import static org.taverna.server.output_description.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlSeeAlso( { ErrorValue.class, LeafValue.class, ListValue.class, AbsentValue.class })
public abstract class AbstractValue {
	@XmlAttribute
	public String output;
	@XmlAttribute(namespace = XLINK)
	public URI href;
	@XmlAttribute(namespace = RDF)
	public String about;

	public void setAddress(URI uri, String localAddress) {
		if (uri.getPath().endsWith("/")) {
			href = URI.create(uri + "wd/out/" + localAddress);
		} else {
			href = URI.create(uri + "/wd/out/" + localAddress);
		}
		about = "out/" + localAddress;
	}
}
