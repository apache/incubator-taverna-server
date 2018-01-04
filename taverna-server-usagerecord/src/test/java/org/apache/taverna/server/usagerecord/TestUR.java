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
import static java.lang.Runtime.getRuntime;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.apache.taverna.server.usagerecord.xml.urf.Charge;
import org.apache.taverna.server.usagerecord.xml.urf.CpuDuration;
import org.apache.taverna.server.usagerecord.xml.urf.Disk;
import org.apache.taverna.server.usagerecord.xml.urf.EndTime;
import org.apache.taverna.server.usagerecord.xml.urf.Host;
import org.apache.taverna.server.usagerecord.xml.urf.JobIdentity;
import org.apache.taverna.server.usagerecord.xml.urf.JobName;
import org.apache.taverna.server.usagerecord.xml.urf.MachineName;
import org.apache.taverna.server.usagerecord.xml.urf.Memory;
import org.apache.taverna.server.usagerecord.xml.urf.Network;
import org.apache.taverna.server.usagerecord.xml.urf.NodeCount;
import org.apache.taverna.server.usagerecord.xml.urf.PhaseResource;
import org.apache.taverna.server.usagerecord.xml.urf.Processors;
import org.apache.taverna.server.usagerecord.xml.urf.ProjectName;
import org.apache.taverna.server.usagerecord.xml.urf.Queue;
import org.apache.taverna.server.usagerecord.xml.urf.RecordIdentity;
import org.apache.taverna.server.usagerecord.xml.urf.ServiceLevel;
import org.apache.taverna.server.usagerecord.xml.urf.StartTime;
import org.apache.taverna.server.usagerecord.xml.urf.Status;
import org.apache.taverna.server.usagerecord.xml.urf.SubmitHost;
import org.apache.taverna.server.usagerecord.xml.urf.Swap;
import org.apache.taverna.server.usagerecord.xml.urf.TimeDuration;
import org.apache.taverna.server.usagerecord.xml.urf.TimeInstant;
import org.apache.taverna.server.usagerecord.xml.urf.UserIdentity;
import org.apache.taverna.server.usagerecord.xml.urf.VolumeResource;
import org.apache.taverna.server.usagerecord.xml.urf.WallDuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestUR {
	SchemaOutputResolver sink;
	StringWriter writer;

	String result() {
		return writer.toString();
	}

	@Before
	public void setUp() throws Exception {
		writer = new StringWriter();
		sink = new SchemaOutputResolver() {
			@Override
			public Result createOutput(String namespaceUri,
					String suggestedFileName) throws IOException {
				StreamResult sr = new StreamResult(writer);
				sr.setSystemId("/dev/null");
				return sr;
			}
		};
		Assert.assertNull(null);// Shut up, Eclipse!
		Assert.assertEquals("", result());
	}

	@Test
	public void testSchema() throws JAXBException, IOException {
		JAXBContext.newInstance(JobUsageRecord.class).generateSchema(sink);
		Assert.assertNotSame("", result());
	}

	@Test
	public void testSchemaCompleteness() throws JAXBException, DatatypeConfigurationException {
		JAXBIntrospector info = JAXBContext.newInstance(JobUsageRecord.class).createJAXBIntrospector();
		Assert.assertTrue(info.isElement(new Charge()));
		Assert.assertTrue(info.isElement(new CpuDuration()));
		Assert.assertTrue(info.isElement(new Disk()));
		Assert.assertTrue(info.isElement(new EndTime()));
		Assert.assertTrue(info.isElement(new Host()));
		Assert.assertTrue(info.isElement(new JobIdentity()));
		Assert.assertTrue(info.isElement(new JobName()));
		Assert.assertTrue(info.isElement(new JobUsageRecord()));
		Assert.assertTrue(info.isElement(new MachineName()));
		Assert.assertTrue(info.isElement(new Memory()));
		Assert.assertTrue(info.isElement(new Network()));
		Assert.assertTrue(info.isElement(new NodeCount()));
		Assert.assertTrue(info.isElement(new PhaseResource()));
		Assert.assertTrue(info.isElement(new Processors()));
		Assert.assertTrue(info.isElement(new ProjectName()));
		Assert.assertTrue(info.isElement(new Queue()));
		Assert.assertTrue(info.isElement(new RecordIdentity()));
		Assert.assertTrue(info.isElement(new ServiceLevel()));
		Assert.assertTrue(info.isElement(new StartTime()));
		Assert.assertTrue(info.isElement(new Status()));
		Assert.assertTrue(info.isElement(new SubmitHost()));
		Assert.assertTrue(info.isElement(new Swap()));
		Assert.assertTrue(info.isElement(new TimeDuration()));
		Assert.assertTrue(info.isElement(new TimeInstant()));
		Assert.assertTrue(info.isElement(new UserIdentity()));
		Assert.assertTrue(info.isElement(new VolumeResource()));
		Assert.assertTrue(info.isElement(new WallDuration()));
	}

	@Test
	public void testGenerate() throws DatatypeConfigurationException,
			JAXBException {
		JobUsageRecord ur = new JobUsageRecord();
		ur.setStatus("Completed");
		ur.addWallDuration(1000 * 65);
		ur.addHost("localhost");
		ur.addMemory(getRuntime().totalMemory() - getRuntime().freeMemory()).setType("vm");

		String record = ur.marshal();
		Assert.assertNotSame("", record);
		//System.out.println(record);
	}
}
