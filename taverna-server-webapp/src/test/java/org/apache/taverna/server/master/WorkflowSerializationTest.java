package org.apache.taverna.server.master;
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

import static org.apache.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW_NS;
import static org.apache.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW_ROOTNAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.apache.taverna.server.master.common.Workflow;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WorkflowSerializationTest {
	@Test
	public void testWorkflowSerialization()
			throws ParserConfigurationException, IOException,
			ClassNotFoundException {
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document doc = db.getDOMImplementation().createDocument(null, null,
				null);
		Element workflow = doc.createElementNS(T2FLOW_NS, T2FLOW_ROOTNAME);
		Element foo = doc.createElementNS("urn:foo:bar", "pqr:foo");
		foo.setTextContent("bar");
		foo.setAttribute("xyz", "abc");
		workflow.appendChild(foo);
		Workflow w = new Workflow(workflow);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(w);
		}

		Object o;
		try (ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(baos.toByteArray()))) {
			o = ois.readObject();
		}

		Assert.assertNotNull(o);
		Assert.assertEquals(w.getClass(), o.getClass());
		Workflow w2 = (Workflow) o;
		Assert.assertNotNull(w2.getT2flowWorkflow());
		Element e = w2.getT2flowWorkflow();
		Assert.assertEquals(T2FLOW_ROOTNAME, e.getLocalName());
		Assert.assertEquals(T2FLOW_NS, e.getNamespaceURI());
		e = (Element) e.getFirstChild();
		Assert.assertEquals("foo", e.getLocalName());
		Assert.assertEquals("pqr", e.getPrefix());
		Assert.assertEquals("urn:foo:bar", e.getNamespaceURI());
		Assert.assertEquals("bar", e.getTextContent());
		Assert.assertEquals(1, e.getChildNodes().getLength());
		// WARNING: These are dependent on how namespaces are encoded!
		Assert.assertEquals(2, e.getAttributes().getLength());
		Assert.assertEquals("xyz", ((Attr) e.getAttributes().item(1)).getLocalName());
		Assert.assertEquals("abc", e.getAttribute("xyz"));
	}
}
