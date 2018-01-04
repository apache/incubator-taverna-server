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
package org.apache.taverna.server.usagerecord.xml.urf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>JobUsageRecord</code>
 * <p>
 * For constructing <code>JobUsageRecord</code>s it is recommended to use the
 * convenience class
 * {@link org.apache.taverna.server.usagerecord.JobUsageRecord} 
 * instead of this class.
 * 
 * @see org.apache.taverna.server.usagerecord.JobUsageRecord
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page=18">GFD.98 section 8.2</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class JobUsageRecord
    extends UsageRecordType
{
}
