/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.port_description.utils;

import static javax.xml.bind.DatatypeConverter.parseInt;
import static javax.xml.bind.DatatypeConverter.printInt;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A type conversion utility for use with JAXB.
 * 
 * @author Donal Fellows
 */
public class IntAdapter extends XmlAdapter<String, Integer> {
	@Override
	public String marshal(Integer value) throws Exception {
		if (value == null)
			return null;
		return printInt(value);
	}

	@Override
	public Integer unmarshal(String value) throws Exception {
		if (value == null)
			return null;
		return parseInt(value);
	}
}
