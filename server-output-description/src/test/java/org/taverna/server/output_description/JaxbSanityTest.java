package org.taverna.server.output_description;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test file ensures that the JAXB bindings will work once deployed instead
 * of mysteriously failing in service.
 * 
 * @author Donal Fellows
 */
public class JaxbSanityTest {
	SchemaOutputResolver sink;
	StringWriter schema;

	String schema() {
		return schema.toString();
	}

	@Before
	public void init() {
		schema = new StringWriter();
		sink = new SchemaOutputResolver() {
			@Override
			public Result createOutput(String namespaceUri,
					String suggestedFileName) throws IOException {
				StreamResult sr = new StreamResult(schema);
				sr.setSystemId("/dev/null");
				return sr;
			}
		};
		Assert.assertNull(null);// Shut up, Eclipse!
		assertEquals("", schema());
	}

	@Test
	public void testJAXBForAbsentValue() throws Exception {
		JAXBContext.newInstance(AbstractValue.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForAbstractValue() throws Exception {
		JAXBContext.newInstance(AbstractValue.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForErrorValue() throws Exception {
		JAXBContext.newInstance(ErrorValue.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForLeafValue() throws Exception {
		JAXBContext.newInstance(LeafValue.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForListValue() throws Exception {
		JAXBContext.newInstance(ListValue.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForOutputs() throws Exception {
		JAXBContext.newInstance(Outputs.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForRdfWrapper() throws Exception {
		JAXBContext.newInstance(RdfWrapper.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForEverythingAtOnce() throws Exception {
		JAXBContext c = JAXBContext.newInstance(AbsentValue.class,
				AbstractValue.class, ListValue.class, LeafValue.class,
				ErrorValue.class, Outputs.class, RdfWrapper.class);
		c.generateSchema(sink);
		// System.out.println(schema());
		assertTrue(schema().length() > 0);
	}
}
