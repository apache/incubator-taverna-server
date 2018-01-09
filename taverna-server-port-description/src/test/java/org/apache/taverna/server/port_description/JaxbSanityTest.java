package org.apache.taverna.server.port_description;
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

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
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

	private String schemaTest(Class<?>... classes) throws IOException, JAXBException {
		Assert.assertTrue(schema().isEmpty());
		JAXBContext.newInstance(classes).generateSchema(sink);
		Assert.assertFalse(schema().isEmpty());
		return schema();
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
	}

	@Test
	public void testJAXBForInput() throws Exception {
		schemaTest(InputDescription.InputPort.class);
	}

	@Test
	public void testJAXBForInputDescription() throws Exception {
		schemaTest(InputDescription.class);
	}

	@Test
	public void testJAXBForAbsentValue() throws Exception {
		schemaTest(AbstractValue.class);
	}

	@Test
	public void testJAXBForAbstractValue() throws Exception {
		schemaTest(AbstractValue.class);
	}

	@Test
	public void testJAXBForErrorValue() throws Exception {
		schemaTest(ErrorValue.class);
	}

	@Test
	public void testJAXBForLeafValue() throws Exception {
		schemaTest(LeafValue.class);
	}

	@Test
	public void testJAXBForListValue() throws Exception {
		schemaTest(ListValue.class);
	}

	@Test
	public void testJAXBForOutputDescription() throws Exception {
		schemaTest(OutputDescription.class);
	}

	@Test
	public void testJAXBForEverythingAtOnce() throws Exception {
		schemaTest(AbsentValue.class, AbstractValue.class, ListValue.class,
				LeafValue.class, ErrorValue.class, OutputDescription.class,
				InputDescription.InputPort.class, InputDescription.class);
		// System.out.println(schema());
	}
}
