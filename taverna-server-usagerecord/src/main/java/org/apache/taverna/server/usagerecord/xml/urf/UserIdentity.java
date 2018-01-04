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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.taverna.server.usagerecord.xml.dsig.KeyInfoType;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>UserIdentity</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page=25">GFD.98 section 11.3</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "localUserId",
    "globalUserName",
    "keyInfo"
})
@XmlRootElement(name = "UserIdentity")
public class UserIdentity {

    @XmlElement(name = "LocalUserId")
    protected String localUserId;
    @XmlElement(name = "GlobalUserName")
    protected String globalUserName;
    @XmlElement(name = "KeyInfo", namespace = "http://www.w3.org/2000/09/xmldsig#")
    protected KeyInfoType keyInfo;

    /**
     * Gets the value of the localUserId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocalUserId() {
        return localUserId;
    }

    /**
     * Sets the value of the localUserId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocalUserId(String value) {
        this.localUserId = value;
    }

    /**
     * Gets the value of the globalUserName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGlobalUserName() {
        return globalUserName;
    }

    /**
     * Sets the value of the globalUserName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGlobalUserName(String value) {
        this.globalUserName = value;
    }

    /**
     * Gets the value of the keyInfo property.
     * 
     * @return
     *     possible object is
     *     {@link KeyInfoType }
     *     
     */
    public KeyInfoType getKeyInfo() {
        return keyInfo;
    }

    /**
     * Sets the value of the keyInfo property.
     * 
     * @param value
     *     allowed object is
     *     {@link KeyInfoType }
     *     
     */
    public void setKeyInfo(KeyInfoType value) {
        this.keyInfo = value;
    }

}
