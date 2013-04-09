/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.taverna.server.localworker.impl.LocalWorker.DO_MKDIR;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.taverna.server.localworker.remote.IllegalStateTransitionException;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteInput;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.server.UsageRecordReceiver;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@SuppressWarnings
public class LocalWorkerTest {
	LocalWorker lw;
	static List<String> events;

	public static RemoteStatus returnThisStatus = RemoteStatus.Operating;

	static class DummyWorker implements Worker {
		@Override
		public RemoteListener getDefaultListener() {
			return new RemoteListener() {
				@Override
				public String getConfiguration() {
					return "RLCONFIG";
				}

				@Override
				public String getName() {
					return "RLNAME";
				}

				@Override
				public String getProperty(String propName) {
					return "RLPROP[" + propName + "]";
				}

				@Override
				public String getType() {
					return "RLTYPE";
				}

				@Override
				public String[] listProperties() {
					return new String[] { "RLP1", "RLP2" };
				}

				@Override
				public void setProperty(String propName, String value) {
					events.add("setProperty[");
					events.add(propName);
					events.add(value);
					events.add("]");
				}
			};
		}

		@Override
		public RemoteStatus getWorkerStatus() {
			events.add("status=" + returnThisStatus);
			return returnThisStatus;
		}

		@Override
		public boolean initWorker(LocalWorker local,
				String executeWorkflowCommand, String workflow,
				File workingDir, File inputBaclava,
				Map<String, File> inputFiles, Map<String, String> inputValues,
				File outputBaclava, File cmdir, char[] cmpass,
				Map<String, String> env, String id, List<String> conf)
				throws Exception {
			events.add("init[");
			events.add(executeWorkflowCommand);
			events.add(workflow);
			int dirLen = workingDir.getName().length();
			events.add(Integer.toString(dirLen));
			events.add(inputBaclava == null ? "<null>" : inputBaclava
					.toString().substring(dirLen));
			Map<String, String> in = new TreeMap<String, String>();
			for (Entry<String, File> name : inputFiles.entrySet()) {
				in.put(name.getKey(), name.getValue() == null ? "<null>" : name
						.getValue().getName());
			}
			events.add(in.toString());
			events.add(new TreeMap<String, String>(inputValues).toString());
			events.add(outputBaclava == null ? "<null>" : outputBaclava
					.getName());
			// TODO: check cmdir and cmpass
			// TODO: log env
			events.add("]");
			return true;
		}

		@Override
		public void killWorker() throws Exception {
			events.add("kill");
		}

		@Override
		public void startWorker() throws Exception {
			events.add("start");
		}

		@Override
		public void stopWorker() throws Exception {
			events.add("stop");
		}

		@Override
		public void setURReceiver(UsageRecordReceiver receiver) {
			// We just ignore this
		}

		@Override
		public void deleteLocalResources() throws ImplementationException {
			// Nothing to do here
		}
	}

	WorkerFactory factory = new WorkerFactory() {
		@Override
		public Worker makeInstance() throws Exception {
			return new DummyWorker();
		}
	};

	@Before
	public void setUp() throws Exception {
		lw = new LocalWorker("XWC", "WF", null, randomUUID(),
				new HashMap<String, String>(), new ArrayList<String>(), factory);
		events = new ArrayList<String>();
		returnThisStatus = RemoteStatus.Operating;
	}

	@After
	public void tearDown() throws Exception {
		lw.destroy();
	}

	private List<String> l(String... strings) {
		return Arrays.asList(strings);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	@Test
	public void testDestroy1() throws Exception {
		lw.destroy();
		assertEquals(l(), events);
	}

	@Test
	public void testDestroy2() throws Exception {
		lw.setStatus(RemoteStatus.Operating);
		lw.destroy();
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "kill"), events);
	}

	@Test
	public void testDestroy3() throws Exception {
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Stopped);
		lw.destroy();
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "stop", "kill"), events);
	}

	@Test
	public void testDestroy4() throws Exception {
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Finished);
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "kill"), events);
		lw.destroy();
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "kill"), events);
	}

	@Test
	public void testAddListener() {
		Throwable t = null;
		try {
			lw.addListener(null);
		} catch (Throwable caught) {
			t = caught;
		}
		assertNotNull(t);
		assertSame(ImplementationException.class, t.getClass());
		assertNotNull(t.getMessage());
		assertEquals("not implemented", t.getMessage());
	}

	@Test
	public void testGetInputBaclavaFile() throws Exception {
		assertNull(lw.getInputBaclavaFile());
		lw.setInputBaclavaFile("IBaclava");
		assertNotNull(lw.getInputBaclavaFile());
		assertEquals("IBaclava", lw.getInputBaclavaFile());
		lw.makeInput("FOO").setValue("BAR");
		assertNull(lw.getInputBaclavaFile());
	}

	@Test
	public void testGetInputsWithValue() throws Exception {
		assertEquals(0, lw.getInputs().size());

		lw.makeInput("FOO").setValue("BAR");

		assertEquals(1, lw.getInputs().size());
		assertEquals("FOO", lw.getInputs().get(0).getName());
		assertNull(lw.getInputs().get(0).getFile());
		assertNotNull(lw.getInputs().get(0).getValue());

		lw.setInputBaclavaFile("BLAH");

		assertEquals(1, lw.getInputs().size());
		assertNull(lw.getInputs().get(0).getFile());
		assertNull(lw.getInputs().get(0).getValue());
	}

	@Test
	public void testGetInputsWithFile() throws Exception {
		assertEquals(0, lw.getInputs().size());

		lw.makeInput("BAR").setFile("FOO");

		assertEquals(1, lw.getInputs().size());
		assertEquals("BAR", lw.getInputs().get(0).getName());
		assertNotNull(lw.getInputs().get(0).getFile());
		assertNull(lw.getInputs().get(0).getValue());

		lw.setInputBaclavaFile("BLAH");

		assertEquals(1, lw.getInputs().size());
		assertNull(lw.getInputs().get(0).getFile());
		assertNull(lw.getInputs().get(0).getValue());
	}

	@Test
	public void testGetListenerTypes() {
		assertEquals("[]", lw.getListenerTypes().toString());
	}

	@Test
	public void testGetListeners() throws Exception {
		assertEquals(1, lw.getListeners().size());
		RemoteListener rl = lw.getListeners().get(0);
		assertEquals("RLNAME", rl.getName());
		assertEquals("RLCONFIG", rl.getConfiguration());
		assertEquals("RLTYPE", rl.getType());
		assertEquals("[RLP1, RLP2]", Arrays.asList(rl.listProperties())
				.toString());
		assertEquals("RLPROP[RLP1]", rl.getProperty("RLP1"));
		assertEquals("RLPROP[RLP2]", rl.getProperty("RLP2"));
		rl.setProperty("FOOBAR", "BARFOO");
		assertEquals(l("setProperty[", "FOOBAR", "BARFOO", "]"), events);
	}

	@Test
	public void testGetOutputBaclavaFile() throws Exception {
		assertNull(lw.getOutputBaclavaFile());
		lw.setOutputBaclavaFile("notnull");
		assertEquals("notnull", lw.getOutputBaclavaFile());
		lw.setOutputBaclavaFile(null);
		assertNull(lw.getOutputBaclavaFile());
	}

	@Test
	public void testGetSecurityContext() throws Exception {
		boolean md = DO_MKDIR;
		LocalWorker.DO_MKDIR = false; // HACK! Work around Hudson problem...
		try {
			assertNotNull(lw.getSecurityContext());
		} finally {
			LocalWorker.DO_MKDIR = md;
		}
	}

	@Test
	public void testGetStatusInitial() {
		assertEquals(RemoteStatus.Initialized, lw.getStatus());
	}

	@Test
	public void testGetStatus() throws Exception {
		assertEquals(RemoteStatus.Initialized, lw.getStatus());
		returnThisStatus = RemoteStatus.Operating;
		assertEquals(RemoteStatus.Initialized, lw.getStatus());
		lw.setStatus(RemoteStatus.Operating);
		assertEquals(RemoteStatus.Operating, lw.getStatus());
		assertEquals(RemoteStatus.Operating, lw.getStatus());
		returnThisStatus = RemoteStatus.Finished;
		assertEquals(RemoteStatus.Finished, lw.getStatus());
		returnThisStatus = RemoteStatus.Stopped;
		assertEquals(RemoteStatus.Finished, lw.getStatus());
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "status=Operating", "status=Operating",
						"status=Finished"), events);
	}

	@Test
	public void testGetWorkingDirectory() throws Exception {
		RemoteDirectory rd = lw.getWorkingDirectory();
		assertNotNull(rd);
		assertNotNull(rd.getContents());
		assertNull(rd.getContainingDirectory());
		assertNotNull(rd.getName());
		assertEquals(-1, rd.getName().indexOf('/'));
		assertFalse("..".equals(rd.getName()));
		assertEquals("", rd.getName());
	}

	@Test
	public void testValidateFilename() throws Exception {
		lw.validateFilename("foobar");
		lw.validateFilename("foo/bar");
		lw.validateFilename("foo.bar");
		lw.validateFilename("foo..bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad0() throws Exception {
		lw.validateFilename("./.");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad1() throws Exception {
		lw.validateFilename("/");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad2() throws Exception {
		lw.validateFilename("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad3() throws Exception {
		lw.validateFilename(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad4() throws Exception {
		lw.validateFilename("..");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateFilenameBad5() throws Exception {
		lw.validateFilename("foo/../bar");
	}

	@Test
	public void testMakeInput() throws Exception {
		assertEquals(0, lw.getInputs().size());

		RemoteInput ri = lw.makeInput("TEST");

		assertNotNull(ri);
		assertEquals(1, lw.getInputs().size());
		assertNotSame(ri, lw.getInputs().get(0)); // different delegates
		assertEquals("TEST", ri.getName());
		assertNull(ri.getFile());
		assertNull(ri.getValue());

		lw.setInputBaclavaFile("bad");
		ri.setFile("good");
		assertEquals("good", ri.getFile());
		assertNull(lw.getInputBaclavaFile());
		ri.setValue("very good");
		assertEquals("very good", ri.getValue());
		assertNull(ri.getFile());
		assertNull(lw.getInputBaclavaFile());

		lw.makeInput("TEST2");
		assertEquals(2, lw.getInputs().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMakeInputFileSanity() throws Exception {
		lw.makeInput("foo").setFile("/../bar");
	}

	@Test
	public void testMakeListener() {
		Throwable t = null;
		try {
			lw.makeListener("?", "?");
		} catch (Throwable caught) {
			t = caught;
		}
		assertNotNull(t);
		assertSame(RemoteException.class, t.getClass());
		assertNotNull(t.getMessage());
		assertEquals("listener manufacturing unsupported", t.getMessage());
	}

	@Test
	public void testSetInputBaclavaFile1() throws Exception {
		assertNull(lw.getInputBaclavaFile());
		lw.setInputBaclavaFile("eg");
		assertEquals("eg", lw.getInputBaclavaFile());
	}

	@Test
	public void testSetInputBaclavaFile2() throws Exception {
		RemoteInput ri = lw.makeInput("foo");
		ri.setValue("bar");
		assertEquals("bar", ri.getValue());
		lw.setInputBaclavaFile("eg");
		assertNull(ri.getValue());
	}

	@Test
	public void testSetOutputBaclavaFile1() throws Exception {
		assertNull(lw.outputBaclava);
		lw.setOutputBaclavaFile("foobar");
		assertEquals("foobar", lw.outputBaclava);
		assertEquals("foobar", lw.getOutputBaclavaFile());
		lw.setOutputBaclavaFile("foo/bar");
		assertEquals("foo/bar", lw.outputBaclava);
		assertEquals("foo/bar", lw.getOutputBaclavaFile());
		lw.setOutputBaclavaFile(null);
		assertNull(lw.outputBaclava);
		assertNull(lw.getOutputBaclavaFile());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetOutputBaclavaFile2() throws Exception {
		lw.setOutputBaclavaFile("/foobar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetOutputBaclavaFile3() throws Exception {
		lw.setOutputBaclavaFile("foo/../bar");
	}

	@Test
	public void testSetStatus0() throws Exception {
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Stopped);
		lw.setStatus(RemoteStatus.Stopped);
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Finished);
		lw.setStatus(RemoteStatus.Finished);
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "stop", "start", "kill"), events);
	}

	@Test
	public void testSetStatus1() throws Exception {
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Stopped);
		lw.setStatus(RemoteStatus.Finished);
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>", "{}", "{}", "<null>",
						"]", "stop", "kill"), events);
	}

	@Test
	public void testSetStatus2() throws Exception {
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Finished);
		assertEquals(l(), events);
	}

	@Test(expected = IllegalStateTransitionException.class)
	public void testSetStatus3() throws Exception {
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Finished);
		lw.setStatus(RemoteStatus.Initialized);
	}

	@Test(expected = IllegalStateTransitionException.class)
	public void testSetStatus4() throws Exception {
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Initialized);
	}

	@Test(expected = IllegalStateTransitionException.class)
	public void testSetStatus5() throws Exception {
		lw.setStatus(RemoteStatus.Initialized);
		lw.setStatus(RemoteStatus.Stopped);
	}

	@Test(expected = IllegalStateTransitionException.class)
	public void testSetStatus6() throws Exception {
		lw.setStatus(RemoteStatus.Finished);
		lw.setStatus(RemoteStatus.Stopped);
	}

	@Test(expected = IllegalStateTransitionException.class)
	public void testSetStatus7() throws Exception {
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Stopped);
		lw.setStatus(RemoteStatus.Initialized);
	}

	@Test
	public void testLifecycle() throws Exception {
		lw.makeInput("foo").setFile("foofile");
		lw.makeInput("bar").setValue("barvalue");
		lw.setOutputBaclavaFile("spong");
		lw.setOutputBaclavaFile("boo");
		lw.setStatus(RemoteStatus.Operating);
		lw.setStatus(RemoteStatus.Finished);
		// Assumes order of map, so fragile but works...
		assertEquals(
				l("init[", "XWC", "WF", "36", "<null>",
						"{bar=<null>, foo=foofile}",
						"{bar=barvalue, foo=null}", "boo", "]", "kill"), events);
	}
}