import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;
import org.taverna.server.execution_delegate.RemoteExecution.WorkflowReportDocument;

public class SerializationTest {
	private static final boolean PRINT = true;
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
		assertEquals("", schema());
	}

	@Test
	public void testSchemaGeneration() throws JAXBException, IOException {
		JAXBContext.newInstance(WorkflowReportDocument.class).generateSchema(
				sink);
		assertFalse("generated schema must be non-empty", schema().isEmpty());
		assertTrue(
				"generated schema must define workflowReport element",
				schema().contains(
						"<xs:element name=\"workflowReport\" type=\"WorkflowReport\"/>\n"));
		if (PRINT)
			System.out.print(schema());
	}
}
