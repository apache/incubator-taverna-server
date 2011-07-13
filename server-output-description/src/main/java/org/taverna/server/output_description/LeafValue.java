/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.output_description;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class LeafValue extends AbstractValue {
	@XmlAttribute
	public String contentType;
	@XmlAttribute
	public Long byteLength;
}
