package org.taverna.server.input_description;

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
	public void testJAXBForInput() throws Exception {
		JAXBContext.newInstance(Input.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForInputDescription() throws Exception {
		JAXBContext.newInstance(InputDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForEverythingAtOnce() throws Exception {
		JAXBContext c = JAXBContext.newInstance(Input.class,
				InputDescription.class);
		c.generateSchema(sink);
		// System.out.println(schema());
		assertTrue(schema().length() > 0);
	}
}
