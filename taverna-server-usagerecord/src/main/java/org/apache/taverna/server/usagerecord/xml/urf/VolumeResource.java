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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>VolumeResource</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page-38">GFD.98 section 13.2.4</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "VolumeResource")
public class VolumeResource
    extends ConsumableResourceType
{

    @XmlAttribute(name = "storageUnit", namespace = "http://schema.ogf.org/urf/2003/09/urf")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String storageUnit;

    /**
     * Gets the value of the storageUnit property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStorageUnit() {
        return storageUnit;
    }

    /**
     * Sets the value of the storageUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStorageUnit(String value) {
        this.storageUnit = value;
    }

}
