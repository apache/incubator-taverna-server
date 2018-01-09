/*
 */
package org.apache.taverna.server.port_description.utils;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
