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
 * Open Grid Forum GFD.98 Usage Record Format JAXB elements.
 * <p>
 * JAXB beans for the <em>Usage Record Format</em> as described by 
 * <a href="Open Grid Forum">https://www.ogf.org/</a> (OGF)
 * specification <a href="https://www.ogf.org/documents/GFD.98.pdf">GFD.98</a>
 * serialized in the namespace
 * <code>http://schema.ogf.org/urf/2003/09/urf</code>
 * <p>
 * This package provides the underlying JAXB elements and types for marshalling
 * and unmarshalling XML according to the Usage Record Format. 
 * <p>
 * For constructing <code>JobUsageRecord</code>s it is recommended to use the
 * convenience class
 * {@link org.apache.taverna.server.usagerecord.JobUsageRecord} 
 * instead of this package.
 * <p>
 * The classes in this package are derived from the XML schema <a href=
 * "http://schemas.ogf.org/urf/2003/09/url.xml">http://schemas.ogf.org/urf/2003/09/url.xml</a>
 * <!-- NOTE: typo "url" instead of "urf" upstream -->
 * <p>
 * <blockquote> Usage Record Working Group XML Schema definition (GFD.98)
 * <p>
 * Copyright (C) Open Grid Forum (2006-2007). All Rights Reserved.
 * <p>
 * This document and translations of it may be copied and furnished to others,
 * and derivative works that comment on or otherwise explain it or assist in its
 * implementation may be prepared, copied, published and distributed, in whole
 * or in part, without restriction of any kind, provided that the above
 * copyright notice and this paragraph are included on all such copies and
 * derivative works. However, this document itself may not be modified in any
 * way, such as by removing the copyright notice or references to the OGF or
 * other organizations, except as needed for the purpose of developing Grid
 * Recommendations in which case the procedures for copyrights defined in the
 * OGF Document process must be followed, or as required to translate it into
 * languages other than English.
 * <p>
 * The limited permissions granted above are perpetual and will not be revoked
 * by the OGF or its successors or assignees.
 * <p>
 * This document and the information contained herein is provided on an "As Is"
 * basis and the OGF disclaims all warranties, express or implied, including but
 * not limited to any warranty that the use of the information herein will not
 * infringe any rights or any implied warranties of merchantability or fitness
 * for a particular purpose. </blockquote>
 * 
 * @see org.apache.taverna.server.usagerecord.xml.urf.ObjectFactory
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf">GFD.98</a>
 */
@javax.xml.bind.annotation.XmlSchema(namespace = "http://schema.ogf.org/urf/2003/09/urf", elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED)
package org.apache.taverna.server.usagerecord.xml.urf;
