import static java.lang.Runtime.getRuntime;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ogf.usage.JobUsageRecord;

public class TestUR {
	SchemaOutputResolver sink;
	StringWriter writer;
	DatatypeFactory datatypeFactory;

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
		datatypeFactory = DatatypeFactory.newInstance();
	}

	@Test
	public void testSchema() throws JAXBException, IOException {
		JAXBContext.newInstance(JobUsageRecord.class).generateSchema(sink);
		Assert.assertNotSame("", result());
	}

	@Test
	public void testGenerate() throws DatatypeConfigurationException,
			JAXBException {
		JobUsageRecord ur = new JobUsageRecord();
		ur.setStatus("Completed");
		ur.addWallDuration(1000 * 65);
		ur.addHost("localhost");
		ur.addMemory(getRuntime().totalMemory() - getRuntime().freeMemory()).setType("vm");

		JAXBContext.newInstance(JobUsageRecord.class).createMarshaller()
				.marshal(ur, writer);
		Assert.assertNotSame("", result());
		//System.out.println(result());
	}
}
