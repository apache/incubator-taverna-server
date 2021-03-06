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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.01.04 at 04:15:49 PM GMT 
//


package org.apache.taverna.server.usagerecord.xml.urf2;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.apache.taverna.server.usagerecord.xml.urf2 package. 
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

    private final static QName _Usage_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Usage");
    private final static QName _UsageRecord_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "UsageRecord");
    private final static QName _RecordIdentityBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "RecordIdentityBlock");
    private final static QName _SubjectIdentityBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "SubjectIdentityBlock");
    private final static QName _ComputeUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ComputeUsageBlock");
    private final static QName _JobUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "JobUsageBlock");
    private final static QName _MemoryUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MemoryUsageBlock");
    private final static QName _StorageUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageUsageBlock");
    private final static QName _CloudUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "CloudUsageBlock");
    private final static QName _NetworkUsageBlock_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "NetworkUsageBlock");
    private final static QName _RecordId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "RecordId");
    private final static QName _CreateTime_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "CreateTime");
    private final static QName _Site_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Site");
    private final static QName _Infrastructure_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Infrastructure");
    private final static QName _LocalUserId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "LocalUserId");
    private final static QName _LocalGroupId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "LocalGroupId");
    private final static QName _GlobalUserId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "GlobalUserId");
    private final static QName _GlobalGroupId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "GlobalGroupId");
    private final static QName _GlobalGroupAttribute_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "GlobalGroupAttribute");
    private final static QName _CpuDuration_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "CpuDuration");
    private final static QName _WallDuration_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "WallDuration");
    private final static QName _StartTime_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StartTime");
    private final static QName _EndTime_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "EndTime");
    private final static QName _ExecutionHost_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ExecutionHost");
    private final static QName _HostType_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "HostType");
    private final static QName _Processors_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Processors");
    private final static QName _NodeCount_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "NodeCount");
    private final static QName _ExitStatus_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ExitStatus");
    private final static QName _Charge_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Charge");
    private final static QName _Hostname_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Hostname");
    private final static QName _ProcessId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ProcessId");
    private final static QName _Benchmark_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Benchmark");
    private final static QName _GlobalJobId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "GlobalJobId");
    private final static QName _LocalJobId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "LocalJobId");
    private final static QName _JobName_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "JobName");
    private final static QName _MachineName_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MachineName");
    private final static QName _SubmitHost_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "SubmitHost");
    private final static QName _SubmitType_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "SubmitType");
    private final static QName _Queue_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Queue");
    private final static QName _TimeInstant_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "TimeInstant");
    private final static QName _ServiceLevel_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ServiceLevel");
    private final static QName _Status_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Status");
    private final static QName _MemoryClass_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MemoryClass");
    private final static QName _MemoryResourceCapacityUsed_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MemoryResourceCapacityUsed");
    private final static QName _MemoryResourceCapacityAllocated_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MemoryResourceCapacityAllocated");
    private final static QName _MemoryResourceCapacityRequested_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "MemoryResourceCapacityRequested");
    private final static QName _StorageShare_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageShare");
    private final static QName _StorageMedia_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageMedia");
    private final static QName _StorageClass_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageClass");
    private final static QName _DirectoryPath_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "DirectoryPath");
    private final static QName _FileCount_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "FileCount");
    private final static QName _StorageResourceCapacityUsed_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageResourceCapacityUsed");
    private final static QName _StorageLogicalCapacityUsed_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageLogicalCapacityUsed");
    private final static QName _StorageResourceCapacityAllocated_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "StorageResourceCapacityAllocated");
    private final static QName _Host_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "Host");
    private final static QName _LocalVirtualMachineId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "LocalVirtualMachineId");
    private final static QName _GlobalVirtualMachineId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "GlobalVirtualMachineId");
    private final static QName _SuspendDuration_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "SuspendDuration");
    private final static QName _ImageId_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "ImageId");
    private final static QName _NetworkClass_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "NetworkClass");
    private final static QName _NetworkInboundUsed_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "NetworkInboundUsed");
    private final static QName _NetworkOutboundUsed_QNAME = new QName("http://schema.ogf.org/urf/2013/04/urf", "NetworkOutboundUsed");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.apache.taverna.server.usagerecord.xml.urf2
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
     * Create an instance of {@link UsageRecords }
     * 
     */
    public UsageRecords createUsageRecords() {
        return new UsageRecords();
    }

    /**
     * Create an instance of {@link RecordIdentityBlockType }
     * 
     */
    public RecordIdentityBlockType createRecordIdentityBlockType() {
        return new RecordIdentityBlockType();
    }

    /**
     * Create an instance of {@link SubjectIdentityBlockType }
     * 
     */
    public SubjectIdentityBlockType createSubjectIdentityBlockType() {
        return new SubjectIdentityBlockType();
    }

    /**
     * Create an instance of {@link ComputeUsageBlockType }
     * 
     */
    public ComputeUsageBlockType createComputeUsageBlockType() {
        return new ComputeUsageBlockType();
    }

    /**
     * Create an instance of {@link JobUsageBlockType }
     * 
     */
    public JobUsageBlockType createJobUsageBlockType() {
        return new JobUsageBlockType();
    }

    /**
     * Create an instance of {@link MemoryUsageBlockType }
     * 
     */
    public MemoryUsageBlockType createMemoryUsageBlockType() {
        return new MemoryUsageBlockType();
    }

    /**
     * Create an instance of {@link StorageUsageBlockType }
     * 
     */
    public StorageUsageBlockType createStorageUsageBlockType() {
        return new StorageUsageBlockType();
    }

    /**
     * Create an instance of {@link CloudUsageBlockType }
     * 
     */
    public CloudUsageBlockType createCloudUsageBlockType() {
        return new CloudUsageBlockType();
    }

    /**
     * Create an instance of {@link NetworkUsageBlockType }
     * 
     */
    public NetworkUsageBlockType createNetworkUsageBlockType() {
        return new NetworkUsageBlockType();
    }

    /**
     * Create an instance of {@link InfrastructureType }
     * 
     */
    public InfrastructureType createInfrastructureType() {
        return new InfrastructureType();
    }

    /**
     * Create an instance of {@link GlobalGroupAttributeType }
     * 
     */
    public GlobalGroupAttributeType createGlobalGroupAttributeType() {
        return new GlobalGroupAttributeType();
    }

    /**
     * Create an instance of {@link ExecutionHostType }
     * 
     */
    public ExecutionHostType createExecutionHostType() {
        return new ExecutionHostType();
    }

    /**
     * Create an instance of {@link HostnameType }
     * 
     */
    public HostnameType createHostnameType() {
        return new HostnameType();
    }

    /**
     * Create an instance of {@link BenchmarkType }
     * 
     */
    public BenchmarkType createBenchmarkType() {
        return new BenchmarkType();
    }

    /**
     * Create an instance of {@link SubmitTypeType }
     * 
     */
    public SubmitTypeType createSubmitTypeType() {
        return new SubmitTypeType();
    }

    /**
     * Create an instance of {@link QueueType }
     * 
     */
    public QueueType createQueueType() {
        return new QueueType();
    }

    /**
     * Create an instance of {@link TimeInstantType }
     * 
     */
    public TimeInstantType createTimeInstantType() {
        return new TimeInstantType();
    }

    /**
     * Create an instance of {@link NetworkClassType }
     * 
     */
    public NetworkClassType createNetworkClassType() {
        return new NetworkClassType();
    }

    /**
     * Create an instance of {@link NetworkInboundUsedType }
     * 
     */
    public NetworkInboundUsedType createNetworkInboundUsedType() {
        return new NetworkInboundUsedType();
    }

    /**
     * Create an instance of {@link NetworkOutboundUsedType }
     * 
     */
    public NetworkOutboundUsedType createNetworkOutboundUsedType() {
        return new NetworkOutboundUsedType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Usage")
    public JAXBElement<UsageRecordType> createUsage(UsageRecordType value) {
        return new JAXBElement<UsageRecordType>(_Usage_QNAME, UsageRecordType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UsageRecordType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "UsageRecord", substitutionHeadNamespace = "http://schema.ogf.org/urf/2013/04/urf", substitutionHeadName = "Usage")
    public JAXBElement<UsageRecordType> createUsageRecord(UsageRecordType value) {
        return new JAXBElement<UsageRecordType>(_UsageRecord_QNAME, UsageRecordType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RecordIdentityBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "RecordIdentityBlock")
    public JAXBElement<RecordIdentityBlockType> createRecordIdentityBlock(RecordIdentityBlockType value) {
        return new JAXBElement<RecordIdentityBlockType>(_RecordIdentityBlock_QNAME, RecordIdentityBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SubjectIdentityBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "SubjectIdentityBlock")
    public JAXBElement<SubjectIdentityBlockType> createSubjectIdentityBlock(SubjectIdentityBlockType value) {
        return new JAXBElement<SubjectIdentityBlockType>(_SubjectIdentityBlock_QNAME, SubjectIdentityBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ComputeUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ComputeUsageBlock")
    public JAXBElement<ComputeUsageBlockType> createComputeUsageBlock(ComputeUsageBlockType value) {
        return new JAXBElement<ComputeUsageBlockType>(_ComputeUsageBlock_QNAME, ComputeUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link JobUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "JobUsageBlock")
    public JAXBElement<JobUsageBlockType> createJobUsageBlock(JobUsageBlockType value) {
        return new JAXBElement<JobUsageBlockType>(_JobUsageBlock_QNAME, JobUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MemoryUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MemoryUsageBlock")
    public JAXBElement<MemoryUsageBlockType> createMemoryUsageBlock(MemoryUsageBlockType value) {
        return new JAXBElement<MemoryUsageBlockType>(_MemoryUsageBlock_QNAME, MemoryUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StorageUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageUsageBlock")
    public JAXBElement<StorageUsageBlockType> createStorageUsageBlock(StorageUsageBlockType value) {
        return new JAXBElement<StorageUsageBlockType>(_StorageUsageBlock_QNAME, StorageUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CloudUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "CloudUsageBlock")
    public JAXBElement<CloudUsageBlockType> createCloudUsageBlock(CloudUsageBlockType value) {
        return new JAXBElement<CloudUsageBlockType>(_CloudUsageBlock_QNAME, CloudUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NetworkUsageBlockType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "NetworkUsageBlock")
    public JAXBElement<NetworkUsageBlockType> createNetworkUsageBlock(NetworkUsageBlockType value) {
        return new JAXBElement<NetworkUsageBlockType>(_NetworkUsageBlock_QNAME, NetworkUsageBlockType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "RecordId")
    public JAXBElement<String> createRecordId(String value) {
        return new JAXBElement<String>(_RecordId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "CreateTime")
    public JAXBElement<XMLGregorianCalendar> createCreateTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_CreateTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Site")
    public JAXBElement<String> createSite(String value) {
        return new JAXBElement<String>(_Site_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InfrastructureType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Infrastructure")
    public JAXBElement<InfrastructureType> createInfrastructure(InfrastructureType value) {
        return new JAXBElement<InfrastructureType>(_Infrastructure_QNAME, InfrastructureType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "LocalUserId")
    public JAXBElement<String> createLocalUserId(String value) {
        return new JAXBElement<String>(_LocalUserId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "LocalGroupId")
    public JAXBElement<String> createLocalGroupId(String value) {
        return new JAXBElement<String>(_LocalGroupId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "GlobalUserId")
    public JAXBElement<String> createGlobalUserId(String value) {
        return new JAXBElement<String>(_GlobalUserId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "GlobalGroupId")
    public JAXBElement<String> createGlobalGroupId(String value) {
        return new JAXBElement<String>(_GlobalGroupId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GlobalGroupAttributeType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "GlobalGroupAttribute")
    public JAXBElement<GlobalGroupAttributeType> createGlobalGroupAttribute(GlobalGroupAttributeType value) {
        return new JAXBElement<GlobalGroupAttributeType>(_GlobalGroupAttribute_QNAME, GlobalGroupAttributeType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Duration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "CpuDuration")
    public JAXBElement<Duration> createCpuDuration(Duration value) {
        return new JAXBElement<Duration>(_CpuDuration_QNAME, Duration.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Duration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "WallDuration")
    public JAXBElement<Duration> createWallDuration(Duration value) {
        return new JAXBElement<Duration>(_WallDuration_QNAME, Duration.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StartTime")
    public JAXBElement<XMLGregorianCalendar> createStartTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_StartTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "EndTime")
    public JAXBElement<XMLGregorianCalendar> createEndTime(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_EndTime_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ExecutionHostType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ExecutionHost")
    public JAXBElement<ExecutionHostType> createExecutionHost(ExecutionHostType value) {
        return new JAXBElement<ExecutionHostType>(_ExecutionHost_QNAME, ExecutionHostType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "HostType")
    public JAXBElement<String> createHostType(String value) {
        return new JAXBElement<String>(_HostType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Processors")
    public JAXBElement<BigInteger> createProcessors(BigInteger value) {
        return new JAXBElement<BigInteger>(_Processors_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "NodeCount")
    public JAXBElement<BigInteger> createNodeCount(BigInteger value) {
        return new JAXBElement<BigInteger>(_NodeCount_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ExitStatus")
    public JAXBElement<BigInteger> createExitStatus(BigInteger value) {
        return new JAXBElement<BigInteger>(_ExitStatus_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Charge")
    public JAXBElement<BigDecimal> createCharge(BigDecimal value) {
        return new JAXBElement<BigDecimal>(_Charge_QNAME, BigDecimal.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link HostnameType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Hostname")
    public JAXBElement<HostnameType> createHostname(HostnameType value) {
        return new JAXBElement<HostnameType>(_Hostname_QNAME, HostnameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ProcessId")
    public JAXBElement<BigInteger> createProcessId(BigInteger value) {
        return new JAXBElement<BigInteger>(_ProcessId_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BenchmarkType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Benchmark")
    public JAXBElement<BenchmarkType> createBenchmark(BenchmarkType value) {
        return new JAXBElement<BenchmarkType>(_Benchmark_QNAME, BenchmarkType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "GlobalJobId")
    public JAXBElement<String> createGlobalJobId(String value) {
        return new JAXBElement<String>(_GlobalJobId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "LocalJobId")
    public JAXBElement<String> createLocalJobId(String value) {
        return new JAXBElement<String>(_LocalJobId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "JobName")
    public JAXBElement<String> createJobName(String value) {
        return new JAXBElement<String>(_JobName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MachineName")
    public JAXBElement<String> createMachineName(String value) {
        return new JAXBElement<String>(_MachineName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "SubmitHost")
    public JAXBElement<String> createSubmitHost(String value) {
        return new JAXBElement<String>(_SubmitHost_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SubmitTypeType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "SubmitType")
    public JAXBElement<SubmitTypeType> createSubmitType(SubmitTypeType value) {
        return new JAXBElement<SubmitTypeType>(_SubmitType_QNAME, SubmitTypeType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QueueType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Queue")
    public JAXBElement<QueueType> createQueue(QueueType value) {
        return new JAXBElement<QueueType>(_Queue_QNAME, QueueType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TimeInstantType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "TimeInstant")
    public JAXBElement<TimeInstantType> createTimeInstant(TimeInstantType value) {
        return new JAXBElement<TimeInstantType>(_TimeInstant_QNAME, TimeInstantType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ServiceLevel")
    public JAXBElement<String> createServiceLevel(String value) {
        return new JAXBElement<String>(_ServiceLevel_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Status")
    public JAXBElement<String> createStatus(String value) {
        return new JAXBElement<String>(_Status_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MemoryClass")
    public JAXBElement<String> createMemoryClass(String value) {
        return new JAXBElement<String>(_MemoryClass_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MemoryResourceCapacityUsed")
    public JAXBElement<BigInteger> createMemoryResourceCapacityUsed(BigInteger value) {
        return new JAXBElement<BigInteger>(_MemoryResourceCapacityUsed_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MemoryResourceCapacityAllocated")
    public JAXBElement<BigInteger> createMemoryResourceCapacityAllocated(BigInteger value) {
        return new JAXBElement<BigInteger>(_MemoryResourceCapacityAllocated_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "MemoryResourceCapacityRequested")
    public JAXBElement<BigInteger> createMemoryResourceCapacityRequested(BigInteger value) {
        return new JAXBElement<BigInteger>(_MemoryResourceCapacityRequested_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageShare")
    public JAXBElement<String> createStorageShare(String value) {
        return new JAXBElement<String>(_StorageShare_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageMedia")
    public JAXBElement<String> createStorageMedia(String value) {
        return new JAXBElement<String>(_StorageMedia_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageClass")
    public JAXBElement<String> createStorageClass(String value) {
        return new JAXBElement<String>(_StorageClass_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "DirectoryPath")
    public JAXBElement<String> createDirectoryPath(String value) {
        return new JAXBElement<String>(_DirectoryPath_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "FileCount")
    public JAXBElement<BigInteger> createFileCount(BigInteger value) {
        return new JAXBElement<BigInteger>(_FileCount_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageResourceCapacityUsed")
    public JAXBElement<BigInteger> createStorageResourceCapacityUsed(BigInteger value) {
        return new JAXBElement<BigInteger>(_StorageResourceCapacityUsed_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageLogicalCapacityUsed")
    public JAXBElement<BigInteger> createStorageLogicalCapacityUsed(BigInteger value) {
        return new JAXBElement<BigInteger>(_StorageLogicalCapacityUsed_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "StorageResourceCapacityAllocated")
    public JAXBElement<BigInteger> createStorageResourceCapacityAllocated(BigInteger value) {
        return new JAXBElement<BigInteger>(_StorageResourceCapacityAllocated_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "Host")
    public JAXBElement<String> createHost(String value) {
        return new JAXBElement<String>(_Host_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "LocalVirtualMachineId")
    public JAXBElement<String> createLocalVirtualMachineId(String value) {
        return new JAXBElement<String>(_LocalVirtualMachineId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "GlobalVirtualMachineId")
    public JAXBElement<String> createGlobalVirtualMachineId(String value) {
        return new JAXBElement<String>(_GlobalVirtualMachineId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Duration }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "SuspendDuration")
    public JAXBElement<Duration> createSuspendDuration(Duration value) {
        return new JAXBElement<Duration>(_SuspendDuration_QNAME, Duration.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "ImageId")
    public JAXBElement<String> createImageId(String value) {
        return new JAXBElement<String>(_ImageId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NetworkClassType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "NetworkClass")
    public JAXBElement<NetworkClassType> createNetworkClass(NetworkClassType value) {
        return new JAXBElement<NetworkClassType>(_NetworkClass_QNAME, NetworkClassType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NetworkInboundUsedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "NetworkInboundUsed")
    public JAXBElement<NetworkInboundUsedType> createNetworkInboundUsed(NetworkInboundUsedType value) {
        return new JAXBElement<NetworkInboundUsedType>(_NetworkInboundUsed_QNAME, NetworkInboundUsedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NetworkOutboundUsedType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schema.ogf.org/urf/2013/04/urf", name = "NetworkOutboundUsed")
    public JAXBElement<NetworkOutboundUsedType> createNetworkOutboundUsed(NetworkOutboundUsedType value) {
        return new JAXBElement<NetworkOutboundUsedType>(_NetworkOutboundUsed_QNAME, NetworkOutboundUsedType.class, null, value);
    }

}
