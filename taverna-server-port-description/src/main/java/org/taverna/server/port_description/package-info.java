/*
 */
@XmlSchema(namespace = DATA, elementFormDefault = QUALIFIED, attributeFormDefault = QUALIFIED, xmlns = {
		@XmlNs(prefix = "port", namespaceURI = DATA),
		@XmlNs(prefix = "xlink", namespaceURI = XLINK),
		@XmlNs(prefix = "run", namespaceURI = RUN) })
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

import static javax.xml.bind.annotation.XmlNsForm.QUALIFIED;
import static org.taverna.server.port_description.Namespaces.DATA;
import static org.taverna.server.port_description.Namespaces.RUN;
import static org.taverna.server.port_description.Namespaces.XLINK;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

