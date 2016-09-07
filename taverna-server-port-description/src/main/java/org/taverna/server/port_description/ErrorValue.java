/*
 */
package org.taverna.server.port_description;
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.taverna.server.port_description.utils.IntAdapter;

@XmlType(name = "ErrorValue")
public class ErrorValue extends AbstractValue {
	@XmlAttribute
	@XmlSchemaType(name = "int")
	@XmlJavaTypeAdapter(IntAdapter.class)
	public Integer depth;
	@XmlAttribute(name = "errorFile")
	public String fileName;
	@XmlAttribute(name = "errorByteLength")
	public Long byteLength;
}
