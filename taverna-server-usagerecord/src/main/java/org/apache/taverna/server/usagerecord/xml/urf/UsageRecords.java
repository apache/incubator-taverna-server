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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>UsageRecords</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page=18">GFD.98 section 8.3</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "usage"
})
@XmlRootElement(name = "UsageRecords")
public class UsageRecords {

    @XmlElementRef(name = "Usage", namespace = "http://schema.ogf.org/urf/2003/09/urf", type = JAXBElement.class, required = false)
    protected List<JAXBElement<? extends UsageRecordType>> usage;

    /**
     * Gets the value of the usage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the usage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUsage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}
     * {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}
     * {@link JAXBElement }{@code <}{@link JobUsageRecord }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends UsageRecordType>> getUsage() {
        if (usage == null) {
            usage = new ArrayList<JAXBElement<? extends UsageRecordType>>();
        }
        return this.usage;
    }

}
