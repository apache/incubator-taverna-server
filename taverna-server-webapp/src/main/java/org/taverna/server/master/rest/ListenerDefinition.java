/*
 */
package org.taverna.server.master.rest;
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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Description of what sort of event listener to create and attach to a workflow
 * run. Bound via JAXB.
 * 
 * @author Donal Fellows
 */
@XmlRootElement(name = "listenerDefinition")
@XmlType(name="ListenerDefinition")
public class ListenerDefinition {
	/**
	 * The type of event listener to create.
	 */
	@XmlAttribute
	public String type;
	/**
	 * How the event listener should be configured.
	 */
	@XmlValue
	public String configuration;
}
