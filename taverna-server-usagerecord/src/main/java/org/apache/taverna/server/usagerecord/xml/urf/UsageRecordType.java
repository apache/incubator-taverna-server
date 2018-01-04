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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * Open Grid Forum GFD.98 Usage Record type <code>UsageRecordType</code>
 * 
 * @see <a href="https://www.ogf.org/documents/GFD.98.pdf#page=21">GFD.98 section 10.1</a>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UsageRecordType", propOrder = {
    "recordIdentity",
    "jobIdentity",
    "userIdentity",
    "jobName",
    "charge",
    "status",
    "diskOrMemoryOrSwap",
    "wallDurationOrCpuDurationOrNodeCount"
})
@XmlSeeAlso({
    JobUsageRecord.class
})
public class UsageRecordType {

    @XmlElement(name = "RecordIdentity", required = true)
    protected RecordIdentity recordIdentity;
    @XmlElement(name = "JobIdentity")
    protected JobIdentity jobIdentity;
    @XmlElement(name = "UserIdentity")
    protected List<UserIdentity> userIdentity;
    @XmlElement(name = "JobName")
    protected JobName jobName;
    @XmlElement(name = "Charge")
    protected Charge charge;
    @XmlElement(name = "Status", required = true)
    protected Status status;
    @XmlElements({
        @XmlElement(name = "Disk", type = Disk.class),
        @XmlElement(name = "Memory", type = Memory.class),
        @XmlElement(name = "Swap", type = Swap.class),
        @XmlElement(name = "Network", type = Network.class),
        @XmlElement(name = "TimeDuration", type = TimeDuration.class),
        @XmlElement(name = "TimeInstant", type = TimeInstant.class),
        @XmlElement(name = "ServiceLevel", type = ServiceLevel.class)
    })
    protected List<Object> diskOrMemoryOrSwap;
    @XmlElements({
        @XmlElement(name = "WallDuration", type = WallDuration.class),
        @XmlElement(name = "CpuDuration", type = CpuDuration.class),
        @XmlElement(name = "NodeCount", type = NodeCount.class),
        @XmlElement(name = "Processors", type = Processors.class),
        @XmlElement(name = "EndTime", type = EndTime.class),
        @XmlElement(name = "StartTime", type = StartTime.class),
        @XmlElement(name = "MachineName", type = MachineName.class),
        @XmlElement(name = "SubmitHost", type = SubmitHost.class),
        @XmlElement(name = "Queue", type = Queue.class),
        @XmlElement(name = "ProjectName", type = ProjectName.class),
        @XmlElement(name = "Host", type = Host.class),
        @XmlElement(name = "PhaseResource", type = PhaseResource.class),
        @XmlElement(name = "VolumeResource", type = VolumeResource.class),
        @XmlElement(name = "Resource", type = ResourceType.class),
        @XmlElement(name = "ConsumableResource", type = ConsumableResourceType.class)
    })
    protected List<Object> wallDurationOrCpuDurationOrNodeCount;

    /**
     * Gets the value of the recordIdentity property.
     * 
     * @return
     *     possible object is
     *     {@link RecordIdentity }
     *     
     */
    public RecordIdentity getRecordIdentity() {
        return recordIdentity;
    }

    /**
     * Sets the value of the recordIdentity property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordIdentity }
     *     
     */
    public void setRecordIdentity(RecordIdentity value) {
        this.recordIdentity = value;
    }

    /**
     * Gets the value of the jobIdentity property.
     * 
     * @return
     *     possible object is
     *     {@link JobIdentity }
     *     
     */
    public JobIdentity getJobIdentity() {
        return jobIdentity;
    }

    /**
     * Sets the value of the jobIdentity property.
     * 
     * @param value
     *     allowed object is
     *     {@link JobIdentity }
     *     
     */
    public void setJobIdentity(JobIdentity value) {
        this.jobIdentity = value;
    }

    /**
     * Gets the value of the userIdentity property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the userIdentity property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUserIdentity().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link UserIdentity }
     * 
     * 
     */
    public List<UserIdentity> getUserIdentity() {
        if (userIdentity == null) {
            userIdentity = new ArrayList<UserIdentity>();
        }
        return this.userIdentity;
    }

    /**
     * Gets the value of the jobName property.
     * 
     * @return
     *     possible object is
     *     {@link JobName }
     *     
     */
    public JobName getJobName() {
        return jobName;
    }

    /**
     * Sets the value of the jobName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JobName }
     *     
     */
    public void setJobName(JobName value) {
        this.jobName = value;
    }

    /**
     * Gets the value of the charge property.
     * 
     * @return
     *     possible object is
     *     {@link Charge }
     *     
     */
    public Charge getCharge() {
        return charge;
    }

    /**
     * Sets the value of the charge property.
     * 
     * @param value
     *     allowed object is
     *     {@link Charge }
     *     
     */
    public void setCharge(Charge value) {
        this.charge = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link Status }
     *     
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link Status }
     *     
     */
    public void setStatus(Status value) {
        this.status = value;
    }

    /**
     * Gets the value of the diskOrMemoryOrSwap property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the diskOrMemoryOrSwap property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDiskOrMemoryOrSwap().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Disk }
     * {@link Memory }
     * {@link Swap }
     * {@link Network }
     * {@link TimeDuration }
     * {@link TimeInstant }
     * {@link ServiceLevel }
     * 
     * 
     */
    public List<Object> getDiskOrMemoryOrSwap() {
        if (diskOrMemoryOrSwap == null) {
            diskOrMemoryOrSwap = new ArrayList<Object>();
        }
        return this.diskOrMemoryOrSwap;
    }

    /**
     * Gets the value of the wallDurationOrCpuDurationOrNodeCount property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wallDurationOrCpuDurationOrNodeCount property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWallDurationOrCpuDurationOrNodeCount().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WallDuration }
     * {@link CpuDuration }
     * {@link NodeCount }
     * {@link Processors }
     * {@link EndTime }
     * {@link StartTime }
     * {@link MachineName }
     * {@link SubmitHost }
     * {@link Queue }
     * {@link ProjectName }
     * {@link Host }
     * {@link PhaseResource }
     * {@link VolumeResource }
     * {@link ResourceType }
     * {@link ConsumableResourceType }
     * 
     * 
     */
    public List<Object> getWallDurationOrCpuDurationOrNodeCount() {
        if (wallDurationOrCpuDurationOrNodeCount == null) {
            wallDurationOrCpuDurationOrNodeCount = new ArrayList<Object>();
        }
        return this.wallDurationOrCpuDurationOrNodeCount;
    }

}
