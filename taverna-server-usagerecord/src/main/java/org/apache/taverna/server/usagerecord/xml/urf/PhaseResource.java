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
import javax.xml.datatype.Duration;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>PhaseResource</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page-38">GFD.98 section 13.2.3</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "PhaseResource")
public class PhaseResource
    extends ConsumableResourceType
{

    @XmlAttribute(name = "phaseUnit", namespace = "http://schema.ogf.org/urf/2003/09/urf")
    protected Duration phaseUnit;

    /**
     * Gets the value of the phaseUnit property.
     * 
     * @return
     *     possible object is
     *     {@link Duration }
     *     
     */
    public Duration getPhaseUnit() {
        return phaseUnit;
    }

    /**
     * Sets the value of the phaseUnit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Duration }
     *     
     */
    public void setPhaseUnit(Duration value) {
        this.phaseUnit = value;
    }

}
