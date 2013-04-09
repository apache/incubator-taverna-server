/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.ogf.usage;

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

import org.ogf.usage.v1_0.Charge;
import org.ogf.usage.v1_0.ConsumableResourceType;
import org.ogf.usage.v1_0.CpuDuration;
import org.ogf.usage.v1_0.Disk;
import org.ogf.usage.v1_0.EndTime;
import org.ogf.usage.v1_0.Host;
import org.ogf.usage.v1_0.JobName;
import org.ogf.usage.v1_0.MachineName;
import org.ogf.usage.v1_0.Memory;
import org.ogf.usage.v1_0.Network;
import org.ogf.usage.v1_0.NodeCount;
import org.ogf.usage.v1_0.Processors;
import org.ogf.usage.v1_0.ProjectName;
import org.ogf.usage.v1_0.Queue;
import org.ogf.usage.v1_0.RecordIdentity;
import org.ogf.usage.v1_0.ResourceType;
import org.ogf.usage.v1_0.ServiceLevel;
import org.ogf.usage.v1_0.StartTime;
import org.ogf.usage.v1_0.Status;
import org.ogf.usage.v1_0.SubmitHost;
import org.ogf.usage.v1_0.Swap;
import org.ogf.usage.v1_0.TimeDuration;
import org.ogf.usage.v1_0.TimeInstant;
import org.ogf.usage.v1_0.UserIdentity;
import org.ogf.usage.v1_0.WallDuration;

@XmlRootElement(name = "UsageRecord", namespace = "http://schema.ogf.org/urf/2003/09/urf")
public class JobUsageRecord extends org.ogf.usage.v1_0.UsageRecordType {
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

	public void addResource(ResourceType resource) {
		getWallDurationOrCpuDurationOrNodeCount().add(resource);
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

	public static JobUsageRecord unmarshal(String s) throws JAXBException {
		StringReader reader = new StringReader(s);
		return (JobUsageRecord) JAXBContext.newInstance(JobUsageRecord.class)
				.createUnmarshaller().unmarshal(reader);
	}

	// TODO: Add signing support
}
