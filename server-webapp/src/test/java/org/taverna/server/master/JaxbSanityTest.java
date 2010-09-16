package org.taverna.server.master;

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
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.rest.DirectoryContents;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry;
import org.taverna.server.master.rest.TavernaServerREST.PolicyView.PolicyDescription;
import org.taverna.server.master.rest.TavernaServerInputREST.InDesc;
import org.taverna.server.master.rest.TavernaServerInputREST.InputsDescriptor;
import org.taverna.server.master.rest.TavernaServerListenersREST.ListenerDescription;
import org.taverna.server.master.rest.TavernaServerListenersREST.Listeners;
import org.taverna.server.master.rest.TavernaServerListenersREST.Properties;
import org.taverna.server.master.rest.TavernaServerListenersREST.PropertyDescription;
import org.taverna.server.master.rest.TavernaServerREST.PermittedListeners;
import org.taverna.server.master.rest.TavernaServerREST.PermittedWorkflows;
import org.taverna.server.master.rest.TavernaServerREST.PointingRunList;
import org.taverna.server.master.rest.TavernaServerREST.RunList;
import org.taverna.server.master.rest.TavernaServerREST.ServerDescription;
import org.taverna.server.master.rest.TavernaServerRunREST.RunDescription;

/**
 * This test file ensures that the JAXB bindings will work once deployed instead
 * of mysteriously failing in service.
 * 
 * @author Donal Fellows
 */
public class JaxbSanityTest {
	SchemaOutputResolver sink;
	StringWriter schema;
	String schema() {return schema.toString();}

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
		assertEquals("", schema());
	}

	@Test
	public void testJAXBForDirEntryReference() throws Exception {
		JAXBContext.newInstance(DirEntryReference.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForInputDescription() throws Exception {
		JAXBContext.newInstance(InputDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForRunReference() throws Exception {
		JAXBContext.newInstance(RunReference.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForWorkflow() throws Exception {
		JAXBContext.newInstance(Workflow.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForStatus() throws Exception {
		JAXBContext.newInstance(Status.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForUri() throws Exception {
		JAXBContext.newInstance(Uri.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForDirectoryContents() throws Exception {
		JAXBContext.newInstance(DirectoryContents.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForListenerDefinition() throws Exception {
		JAXBContext.newInstance(ListenerDefinition.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForMakeOrUpdateDirEntry() throws Exception {
		JAXBContext.newInstance(MakeOrUpdateDirEntry.class)
				.generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForInDesc() throws Exception {
		JAXBContext.newInstance(InDesc.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForInputsDescriptor() throws Exception {
		JAXBContext.newInstance(InputsDescriptor.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForListenerDescription() throws Exception {
		JAXBContext.newInstance(ListenerDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForListeners() throws Exception {
		JAXBContext.newInstance(Listeners.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForProperties() throws Exception {
		JAXBContext.newInstance(Properties.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForPropertyDescription() throws Exception {
		JAXBContext.newInstance(PropertyDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForPermittedListeners() throws Exception {
		JAXBContext.newInstance(PermittedListeners.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForPermittedWorkflows() throws Exception {
		JAXBContext.newInstance(PermittedWorkflows.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForServerDescription() throws Exception {
		JAXBContext.newInstance(ServerDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForRunDescription() throws Exception {
		JAXBContext.newInstance(RunDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForRunList() throws Exception {
		JAXBContext.newInstance(RunList.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForPointingRunList() throws Exception {
		JAXBContext.newInstance(PointingRunList.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForPolicyDescription() throws Exception {
		JAXBContext.newInstance(PolicyDescription.class).generateSchema(sink);
		assertTrue(schema().length() > 0);
	}

	@Test
	public void testJAXBForEverythingAtOnce() throws Exception {
		JAXBContext c = JAXBContext.newInstance(DirEntryReference.class,
				InputDescription.class, RunReference.class, Workflow.class,
				Status.class, DirectoryContents.class, InDesc.class,
				ListenerDefinition.class, MakeOrUpdateDirEntry.class,
				InputsDescriptor.class, ListenerDescription.class,
				Listeners.class, Properties.class, PropertyDescription.class,
				PermittedListeners.class, PermittedWorkflows.class,
				ServerDescription.class, RunDescription.class, Uri.class,
				RunList.class, PointingRunList.class, PolicyDescription.class);
		c.generateSchema(sink);
		//System.out.println(schema());
		assertTrue(schema().length() > 0);
	}
}
