package org.taverna.server.master;

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
import org.taverna.server.master.common.Workflow;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WorkflowSerializationTest {
	@Test
	public void testWorkflowSerialization()
			throws ParserConfigurationException, IOException,
			ClassNotFoundException {
		Workflow w = new Workflow();
		w.content = new Element[1];
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document doc = db.getDOMImplementation().createDocument(null, null,
				null);
		w.content[0] = doc.createElement("foo");
		w.content[0].setTextContent("bar");
		w.content[0].setAttribute("xyz", "abc");

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
		Assert.assertNotNull(w2.content);
		Assert.assertEquals(1, w2.content.length);
		Element e = w2.content[0];
		Assert.assertEquals("foo", e.getTagName());
		Assert.assertEquals("bar", e.getTextContent());
		Assert.assertEquals(1, e.getChildNodes().getLength());
		Assert.assertEquals("abc", e.getAttribute("xyz"));
	}
}
