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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ogf.usage.JobUsageRecord;
import org.ogf.usage.v1_0.Charge;
import org.ogf.usage.v1_0.CpuDuration;
import org.ogf.usage.v1_0.Disk;
import org.ogf.usage.v1_0.EndTime;
import org.ogf.usage.v1_0.Host;
import org.ogf.usage.v1_0.JobIdentity;
import org.ogf.usage.v1_0.JobName;
import org.ogf.usage.v1_0.MachineName;
import org.ogf.usage.v1_0.Memory;
import org.ogf.usage.v1_0.Network;
import org.ogf.usage.v1_0.NodeCount;
import org.ogf.usage.v1_0.PhaseResource;
import org.ogf.usage.v1_0.Processors;
import org.ogf.usage.v1_0.ProjectName;
import org.ogf.usage.v1_0.Queue;
import org.ogf.usage.v1_0.RecordIdentity;
import org.ogf.usage.v1_0.ServiceLevel;
import org.ogf.usage.v1_0.StartTime;
import org.ogf.usage.v1_0.Status;
import org.ogf.usage.v1_0.SubmitHost;
import org.ogf.usage.v1_0.Swap;
import org.ogf.usage.v1_0.TimeDuration;
import org.ogf.usage.v1_0.TimeInstant;
import org.ogf.usage.v1_0.UserIdentity;
import org.ogf.usage.v1_0.VolumeResource;
import org.ogf.usage.v1_0.WallDuration;

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
