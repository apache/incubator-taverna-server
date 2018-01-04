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
/**
 * Open Grid Forum GFD.98 Usage Record
 * <p>
 * JAXB bean for a a <em>Usage Record</em> as described by Open Grid Forum's
 * specification <a href="https://www.ogf.org/documents/GFD.98.pdf">GFD.98</a>.
 * <p>
 * The main class {@link org.apache.taverna.server.usagerecord.JobUsageRecord} 
 * represents the outer <code>&lt;UsageRecord&gt;</code> element
 * with convenience methods for setting its properties.
 * <p>
 * The underlying JAXB elements and types are covered by
 * the packages {@link org.apache.taverna.server.usagerecord.xml.urf} representing
 * the namespace <code>http://schema.ogf.org/urf/2003/09/urf</code> and  
 * {@link org.apache.taverna.server.usagerecord.xml.dsig} representing
 * the namespace <code>http://www.w3.org/2000/09/xmldsig#</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf">Open Grid Forum specification GFD.98 (Usage Record)</a>
 * @see <a href="https://www.w3.org/TR/xmldsig-core1/">W3C Recommendation XML Signature Syntax and Processing Version 1.1</a> 
 */
@javax.xml.bind.annotation.XmlSchema(namespace = "http://schema.ogf.org/urf/2003/09/urf", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
package org.apache.taverna.server.usagerecord;

import org.apache.taverna.server.usagerecord.xml.urf.ObjectFactory;
