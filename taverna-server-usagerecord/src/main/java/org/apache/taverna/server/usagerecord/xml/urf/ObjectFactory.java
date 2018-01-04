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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.apache.taverna.server.usagerecord.xml.urf package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Usage_QNAME = new QName("http://schema.ogf.org/urf/2003/09/urf", "Usage");
    private final static QName _UsageRecord_QNAME = new QName("http://schema.ogf.org/urf/2003/09/urf", "UsageRecord");
    private final static QName _JobUsageRecord_QNAME = new QName("http://schema.ogf.org/urf/2003/09/urf", "JobUsageRecord");
    private final static QName _Resource_QNAME = new QName("http://schema.ogf.org/urf/2003/09/urf", "Resource");
    private final static QName _ConsumableResource_QNAME = new QName("http://schema.ogf.org/urf/2003/09/urf", "ConsumableResource");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.apache.taverna.server.usagerecord.xml.urf
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link UsageRecordType }
     * 
     */
    public UsageRecordType createUsageRecordType() {
        return new UsageRecordType();
    }

    /**
     * Create an instance of {@link JobUsageRecord }
     * 
     */
    public JobUsageRecord createJobUsageRecord() {
        return new JobUsageRecord();
    }

    /**
     * Create an instance of {@link UsageRecords }
     * 
     */
    public UsageRecords createUsageRecords() {
        return new UsageRecords();
    }

    /**
     * Create an instance of {@link Network }
     * 
     */
    public Network createNetwork() {
        return new Network();
    }

    /**
     * Create an instance of {@link Disk }
     * 
     */
    public Disk createDisk() {
        return new Disk();
    }

    /**
     * Create an instance of {@link Memory }
     * 
     */
    public Memory createMemory() {
        return new Memory();
    }

    /**
     * Create an instance of {@link Swap }
     * 
     */
    public Swap createSwap() {
        return new Swap();
    }

    /**
     * Create an instance of {@link NodeCount }
     * 
     */
    public NodeCount createNodeCount() {
        return new NodeCount();
    }

    /**
     * Create an instance of {@link Processors }
     * 
     */
    public Processors createProcessors() {
        return new Processors();
    }

    /**
     * Create an instance of {@link TimeDuration }
     * 
     */
    public TimeDuration createTimeDuration() {
        return new TimeDuration();
    }

    /**
     * Create an instance of {@link TimeInstant }
     * 
     */
    public TimeInstant createTimeInstant() {
        return new TimeInstant();
    }

    /**
     * Create an instance of {@link ServiceLevel }
     * 
     */
    public ServiceLevel createServiceLevel() {
        return new ServiceLevel();
    }

    /**
     * Create an instance of {@link CpuDuration }
     * 
     */
    public CpuDuration createCpuDuration() {
        return new CpuDuration();
    }

    /**
     * Create an instance of {@link WallDuration }
     * 
     */
    public WallDuration createWallDuration() {
        return new WallDuration();
    }

    /**
     * Create an instance of {@link EndTime }
     * 
     */
    public EndTime createEndTime() {
        return new EndTime();
    }

    /**
     * Create an instance of {@link StartTime }
     * 
     */
    public StartTime createStartTime() {
        return new StartTime();
    }

    /**
     * Create an instance of {@link MachineName }
     * 
     */
    public MachineName createMachineName() {
        return new MachineName();
    }

    /**
     * Create an instance of {@link SubmitHost }
     * 
     */
    public SubmitHost createSubmitHost() {
        return new SubmitHost();
    }

    /**
     * Create an instance of {@link Host }
     * 
     */
    public Host createHost() {
        return new Host();
    }

    /**
     * Create an instance of {@link Queue }
     * 
     */
    public Queue createQueue() {
        return new Queue();
    }

    /**
     * Create an instance of {@link JobName }
     * 
     */
    public JobName createJobName() {
        return new JobName();
    }

    /**
     * Create an instance of {@link ProjectName }
     * 
     */
    public ProjectName createProjectName() {
        return new ProjectName();
    }

    /**
     * Create an instance of {@link Status }
     * 
     */
    public Status createStatus() {
        return new Status();
    }

    /**
     * Create an instance of {@link Charge }
     * 
     */
    public Charge createCharge() {
        return new Charge();
    }

    /**
     * Create an instance of {@link JobIdentity }
     * 
     */
    public JobIdentity createJobIdentity() {
        return new JobIdentity();
    }

    /**
     * Create an instance of {@link UserIdentity }
     * 
     */
    public UserIdentity createUserIdentity() {
        return new UserIdentity();
    }

    /**
     * Create an instance of {@link RecordIdentity }
     * 
     */
    public RecordIdentity createRecordIdentity() {
        return new RecordIdentity();
    }

    /**
     * Create an instance of {@link ResourceType }
     * 
     */
    public ResourceType createResourceType() {
        return new ResourceType();
    }

    /**
     * Create an instance of {@link ConsumableResourceType }
     * 
     */
    public ConsumableResourceType createConsumableResourceType() {
        return new ConsumableResourceType();
    }

    /**
     * Create an instance of {@link PhaseResource }
     * 
     */
    public PhaseResource createPhaseResource() {
        return new PhaseResource();
    }

    /**
     * Create an instance of {@link VolumeResource }
     * 
     */
    public VolumeResource createVolumeResource() {
        return new VolumeResource();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2003/09/urf", name = "Usage")
    public JAXBElement<UsageRecordType> createUsage(UsageRecordType value) {
        return new JAXBElement<UsageRecordType>(_Usage_QNAME, UsageRecordType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2003/09/urf", name = "UsageRecord", substitutionHeadNamespace = "http://schema.ogf.org/urf/2003/09/urf", substitutionHeadName = "Usage")
    public JAXBElement<UsageRecordType> createUsageRecord(UsageRecordType value) {
        return new JAXBElement<UsageRecordType>(_UsageRecord_QNAME, UsageRecordType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobUsageRecord }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2003/09/urf", name = "JobUsageRecord", substitutionHeadNamespace = "http://schema.ogf.org/urf/2003/09/urf", substitutionHeadName = "Usage")
    public JAXBElement<JobUsageRecord> createJobUsageRecord(JobUsageRecord value) {
        return new JAXBElement<JobUsageRecord>(_JobUsageRecord_QNAME, JobUsageRecord.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResourceType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2003/09/urf", name = "Resource")
    public JAXBElement<ResourceType> createResource(ResourceType value) {
        return new JAXBElement<ResourceType>(_Resource_QNAME, ResourceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ConsumableResourceType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2003/09/urf", name = "ConsumableResource")
    public JAXBElement<ConsumableResourceType> createConsumableResource(ConsumableResourceType value) {
        return new JAXBElement<ConsumableResourceType>(_ConsumableResource_QNAME, ConsumableResourceType.class, null, value);
    }

}
