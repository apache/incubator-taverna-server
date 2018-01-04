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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>JobIdentity</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page=25">GFD.98 section 11.2</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "globalJobId",
    "localJobId",
    "processId"
})
@XmlRootElement(name = "JobIdentity")
public class JobIdentity {

    @XmlElement(name = "GlobalJobId")
    protected String globalJobId;
    @XmlElement(name = "LocalJobId")
    protected String localJobId;
    @XmlElement(name = "ProcessId")
    protected List<String> processId;

    /**
     * Gets the value of the globalJobId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGlobalJobId() {
        return globalJobId;
    }

    /**
     * Sets the value of the globalJobId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGlobalJobId(String value) {
        this.globalJobId = value;
    }

    /**
     * Gets the value of the localJobId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocalJobId() {
        return localJobId;
    }

    /**
     * Sets the value of the localJobId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocalJobId(String value) {
        this.localJobId = value;
    }

    /**
     * Gets the value of the processId property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the processId property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProcessId().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getProcessId() {
        if (processId == null) {
            processId = new ArrayList<String>();
        }
        return this.processId;
    }

}
