/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.port_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LeafValue")
public class LeafValue extends AbstractValue {
	@XmlAttribute(name = "contentFile")
	public String fileName;
	@XmlAttribute(name = "contentType")
	public String contentType;
	@XmlAttribute(name = "contentByteLength")
	public Long byteLength;
}
