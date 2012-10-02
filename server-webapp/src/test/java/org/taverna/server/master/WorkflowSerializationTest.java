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
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();
		Document doc = db.getDOMImplementation().createDocument(null, null,
				null);
		Element dummy = doc.createElement("foo");
		dummy.setTextContent("bar");
		dummy.setAttribute("xyz", "abc");
		Workflow w = new Workflow(dummy);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(w);
		oos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object o = ois.readObject();
		ois.close();

		Assert.assertNotNull(o);
		Assert.assertEquals(w.getClass(), o.getClass());
		Workflow w2 = (Workflow) o;
		Assert.assertNotNull(w2.getT2flowWorkflow());
		Element e = w2.getT2flowWorkflow();
		Assert.assertEquals("foo", e.getTagName());
		Assert.assertEquals("bar", e.getTextContent());
		Assert.assertEquals(1, e.getChildNodes().getLength());
		Assert.assertEquals("abc", e.getAttribute("xyz"));
	}
}
