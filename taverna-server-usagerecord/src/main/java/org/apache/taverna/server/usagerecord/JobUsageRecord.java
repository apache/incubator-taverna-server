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
package org.apache.taverna.server.usagerecord;

import static java.util.UUID.randomUUID;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.dom.DOMSource;

import org.apache.taverna.server.usagerecord.xml.urf.Charge;
import org.apache.taverna.server.usagerecord.xml.urf.ConsumableResourceType;
import org.apache.taverna.server.usagerecord.xml.urf.CpuDuration;
import org.apache.taverna.server.usagerecord.xml.urf.Disk;
import org.apache.taverna.server.usagerecord.xml.urf.EndTime;
import org.apache.taverna.server.usagerecord.xml.urf.Host;
import org.apache.taverna.server.usagerecord.xml.urf.JobName;
import org.apache.taverna.server.usagerecord.xml.urf.MachineName;
import org.apache.taverna.server.usagerecord.xml.urf.Memory;
import org.apache.taverna.server.usagerecord.xml.urf.Network;
import org.apache.taverna.server.usagerecord.xml.urf.NodeCount;
import org.apache.taverna.server.usagerecord.xml.urf.Processors;
import org.apache.taverna.server.usagerecord.xml.urf.ProjectName;
import org.apache.taverna.server.usagerecord.xml.urf.Queue;
import org.apache.taverna.server.usagerecord.xml.urf.RecordIdentity;
import org.apache.taverna.server.usagerecord.xml.urf.ResourceType;
import org.apache.taverna.server.usagerecord.xml.urf.ServiceLevel;
import org.apache.taverna.server.usagerecord.xml.urf.StartTime;
import org.apache.taverna.server.usagerecord.xml.urf.Status;
import org.apache.taverna.server.usagerecord.xml.urf.SubmitHost;
import org.apache.taverna.server.usagerecord.xml.urf.Swap;
import org.apache.taverna.server.usagerecord.xml.urf.TimeDuration;
import org.apache.taverna.server.usagerecord.xml.urf.TimeInstant;
import org.apache.taverna.server.usagerecord.xml.urf.UsageRecordType;
import org.apache.taverna.server.usagerecord.xml.urf.UserIdentity;
import org.apache.taverna.server.usagerecord.xml.urf.WallDuration;
import org.w3c.dom.Element;

@XmlRootElement(name = "UsageRecord", namespace = "http://schema.ogf.org/urf/2003/09/urf")
public class JobUsageRecord extends UsageRecordType {
	private static JAXBContext context;
	static {
		try {
			context = JAXBContext.newInstance(JobUsageRecord.class);
		} catch (JAXBException e) {
			throw new RuntimeException("failed to handle JAXB annotated class",
					e);
		}
	}

	public static JobUsageRecord unmarshal(String s) throws JAXBException {
		return (JobUsageRecord) context.createUnmarshaller().unmarshal(
				new StringReader(s));
	}

	public static JobUsageRecord unmarshal(Element elem) throws JAXBException {
		return context.createUnmarshaller()
				.unmarshal(new DOMSource(elem), JobUsageRecord.class)
				.getValue();
	}
	
	/**
	 * Create a new usage record with a random UUID as its identity.
	 * 
	 * @throws DatatypeConfigurationException
	 *             If the factory for XML-relevant datatypes fails to build; not
	 *             expected.
	 */
	public JobUsageRecord() throws DatatypeConfigurationException {
		datatypeFactory = DatatypeFactory.newInstance();
		RecordIdentity recid = new RecordIdentity();
		recid.setRecordId(randomUUID().toString());
		recid.setCreateTime(datatypeFactory
				.newXMLGregorianCalendar(new GregorianCalendar()));
		setRecordIdentity(recid);
	}

	/**
	 * Create a new usage record with a random UUID as its identity.
	 * 
	 * @param name
	 *            The name of the job to which this record pertains.
	 * @throws DatatypeConfigurationException
	 *             If the factory for XML-relevant datatypes fails to build; not
	 *             expected.
	 */
	public JobUsageRecord(String name) throws DatatypeConfigurationException {
		this();
		setJobName(name);
	}

	@XmlTransient
	private DatatypeFactory datatypeFactory;

	public Status setStatus(String status) {
		Status s = new Status();
		s.setValue(status);
		setStatus(s);
		return s;
	}

	public WallDuration addWallDuration(long millis) {
		WallDuration wall = new WallDuration();
		wall.setValue(datatypeFactory.newDuration(millis));
		getWallDurationOrCpuDurationOrNodeCount().add(wall);
		return wall;
	}

	public CpuDuration addCpuDuration(long millis) {
		CpuDuration cpu = new CpuDuration();
		cpu.setValue(datatypeFactory.newDuration(millis));
		getWallDurationOrCpuDurationOrNodeCount().add(cpu);
		return cpu;
	}

	public NodeCount addNodeCount(int nodes) {
		NodeCount nc = new NodeCount();
		nc.setValue(BigInteger.valueOf(nodes));
		getWallDurationOrCpuDurationOrNodeCount().add(nc);
		return nc;
	}

	public Processors addProcessors(int processors) {
		Processors pc = new Processors();
		pc.setValue(BigInteger.valueOf(processors));
		getWallDurationOrCpuDurationOrNodeCount().add(pc);
		return pc;
	}

	public SubmitHost addSubmitHost(String host) {
		SubmitHost sh = new SubmitHost();
		sh.setValue(host);
		getWallDurationOrCpuDurationOrNodeCount().add(sh);
		return sh;
	}

	public Host addHost(String host) {
		Host h = new Host();
		h.setValue(host);
		getWallDurationOrCpuDurationOrNodeCount().add(h);
		return h;
	}

	public MachineName addMachine(String host) {
		MachineName machine = new MachineName();
		machine.setValue(host);
		getWallDurationOrCpuDurationOrNodeCount().add(machine);
		return machine;
	}

	public ProjectName addProject(String project) {
		ProjectName p = new ProjectName();
		p.setValue(project);
		getWallDurationOrCpuDurationOrNodeCount().add(p);
		return p;
	}

	public void addStartAndEnd(Date start, Date end) {
		GregorianCalendar gc;

		gc = new GregorianCalendar();
		gc.setTime(start);
		StartTime st = new StartTime();
		st.setValue(datatypeFactory.newXMLGregorianCalendar(gc));
		getWallDurationOrCpuDurationOrNodeCount().add(st);

		gc = new GregorianCalendar();
		gc.setTime(end);
		EndTime et = new EndTime();
		et.setValue(datatypeFactory.newXMLGregorianCalendar(gc));
		getWallDurationOrCpuDurationOrNodeCount().add(et);
	}

	public Queue addQueue(String queue) {
		Queue q = new Queue();
		q.setValue(queue);
		getWallDurationOrCpuDurationOrNodeCount().add(q);
		return q;
	}

	public void addResource(ConsumableResourceType consumable) {
		getWallDurationOrCpuDurationOrNodeCount().add(consumable);
	}

	public ResourceType addResource(ResourceType resource) {
		getWallDurationOrCpuDurationOrNodeCount().add(resource);
		return resource;
	}

	public ResourceType addResource(String description, String value) {
		ResourceType resource = new ResourceType();
		resource.setDescription(description);
		resource.setValue(value);
		getWallDurationOrCpuDurationOrNodeCount().add(resource);
		return resource;
	}

	public ServiceLevel addServiceLevel(String service) {
		ServiceLevel sl = new ServiceLevel();
		sl.setValue(service);
		getDiskOrMemoryOrSwap().add(sl);
		return sl;
	}

	public Memory addMemory(long memory) {
		Memory mem = new Memory();
		mem.setValue(BigInteger.valueOf(memory));
		getDiskOrMemoryOrSwap().add(mem);
		return mem;
	}

	public TimeInstant addTimestamp(Date timestamp, String type) {
		TimeInstant instant = new TimeInstant();
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(timestamp);
		instant.setValue(datatypeFactory.newXMLGregorianCalendar(gc));
		instant.setType(type);
		getDiskOrMemoryOrSwap().add(instant);
		return instant;
	}

	public TimeDuration addDuration(long millis, String type) {
		TimeDuration duration = new TimeDuration();
		duration.setValue(datatypeFactory.newDuration(millis));
		duration.setType(type);
		getDiskOrMemoryOrSwap().add(duration);
		return duration;
	}

	public Network addNetwork(long value) {
		Network net = new Network();
		net.setValue(BigInteger.valueOf(value));
		getDiskOrMemoryOrSwap().add(net);
		return net;
	}

	public Disk addDisk(long value) {
		Disk disk = new Disk();
		disk.setValue(BigInteger.valueOf(value));
		getDiskOrMemoryOrSwap().add(disk);
		return disk;
	}

	public Swap addSwap(long value) {
		Swap net = new Swap();
		net.setValue(BigInteger.valueOf(value));
		getDiskOrMemoryOrSwap().add(net);
		return net;
	}

	public UserIdentity addUser(String localUID, String globalName) {
		UserIdentity user = new UserIdentity();
		user.setLocalUserId(localUID);
		user.setGlobalUserName(globalName);
		getUserIdentity().add(user);
		return user;
	}

	public JobName setJobName(String name) {
		JobName jn = new JobName();
		jn.setValue(name);
		this.setJobName(jn);
		return jn;
	}

	public Charge addCharge(float value) {
		Charge c = new Charge();
		c.setValue(value);
		this.setCharge(c);
		return c;
	}

	@SuppressWarnings("unchecked")
	public <T> T getOfType(Class<T> clazz) {
		for (Object o : getWallDurationOrCpuDurationOrNodeCount())
			if (clazz.isInstance(o))
				return (T) o;
		for (Object o : getDiskOrMemoryOrSwap())
			if (clazz.isInstance(o))
				return (T) o;
		return null;
	}

	public String marshal() throws JAXBException {
		StringWriter writer = new StringWriter();
		JAXBContext.newInstance(getClass()).createMarshaller()
				.marshal(this, writer);
		return writer.toString();
	}

	// TODO: Add signing support
}
