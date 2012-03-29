/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import static org.taverna.server.port_description.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Value")
@XmlSeeAlso( { ErrorValue.class, LeafValue.class, ListValue.class, AbsentValue.class })
public abstract class AbstractValue {
	@XmlAttribute(namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI href;

	public void setAddress(URI uri, String localAddress) {
		if (uri.getPath().endsWith("/")) {
			href = URI.create(uri + "wd/out/" + localAddress);
		} else {
			href = URI.create(uri + "/wd/out/" + localAddress);
		}
		//about = "out/" + localAddress;
	}
}
