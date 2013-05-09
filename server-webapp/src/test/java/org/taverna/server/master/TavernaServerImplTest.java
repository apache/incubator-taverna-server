package org.taverna.server.master;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.mocks.ExampleRun;
import org.taverna.server.master.mocks.MockPolicy;
import org.taverna.server.master.mocks.SimpleListenerFactory;
import org.taverna.server.master.mocks.SimpleNonpersistentRunStore;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@SuppressWarnings
public class TavernaServerImplTest {
	private TavernaServerImpl server;
	private MockPolicy policy;
	private SimpleNonpersistentRunStore store;
	@java.lang.SuppressWarnings("unused")
	private ExampleRun.Builder runFactory;
	private SimpleListenerFactory lFactory;
	private TavernaServerSupport support;

	private String lrunname;
	private String lrunconf;

	Listener makeListener(TavernaRun run, final String config) {
		lrunname = run.toString();
		lrunconf = config;
		return new Listener() {
			@Override
			public String getConfiguration() {
				return config;
			}

			@Override
			public String getName() {
				return "bar";
			}

			@Override
			public String getProperty(String propName)
					throws NoListenerException {
				throw new NoListenerException();
			}

			@Override
			public String getType() {
				return "foo";
			}

			@Override
			public String[] listProperties() {
				return new String[0];
			}

			@Override
			public void setProperty(String propName, String value)
					throws NoListenerException, BadPropertyValueException {
				throw new NoListenerException();
			}
		};
	}

	@Before
	@SuppressWarnings
	public void wireup() throws Exception {
		// Wire everything up; ought to be done with Spring, but this works...
		server = new TavernaServerImpl() {
			@Override
			protected RunREST makeRunInterface() {
				return new RunREST() {
					@Override
					protected ListenersREST makeListenersInterface() {
						return new ListenersREST() {
							@Override
							protected SingleListenerREST makeListenerInterface() {
								return new SingleListenerREST() {
									@Override
									protected ListenerPropertyREST makePropertyInterface() {
										return new ListenerPropertyREST() {
										};
									}
								};
							}
						};
					}

					@Override
					protected RunSecurityREST makeSecurityInterface() {
						return new RunSecurityREST() {
						};
					}

					@Override
					protected DirectoryREST makeDirectoryInterface() {
						return new DirectoryREST() {
						};
					}

					@Override
					protected InputREST makeInputInterface() {
						return new InputREST() {
						};
					}

					@Override
					protected InteractionFeed makeInteractionFeed() {
						return null; // TODO...
					}
				};
			}

			@Override
			public PolicyView getPolicyDescription() {
				return new PolicyREST();
			}
		};
		support = new TavernaServerSupport();
		server.setSupport(support);
		support.setWebapp(server);
		support.setLogGetPrincipalFailures(false);
		support.setStateModel(new ManagementModel() {
			@Override
			public boolean getAllowNewWorkflowRuns() {
				return true;
			}

			@Override
			public boolean getLogIncomingWorkflows() {
				return false;
			}

			@Override
			public boolean getLogOutgoingExceptions() {
				return false;
			}

			@Override
			public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
			}

			@Override
			public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
			}

			@Override
			public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
			}

			@Override
			public String getUsageRecordLogFile() {
				return null;
			}

			@Override
			public void setUsageRecordLogFile(String usageRecordLogFile) {
			}
		});
		server.setPolicy(policy = new MockPolicy());
		support.setPolicy(policy);
		server.setRunStore(store = new SimpleNonpersistentRunStore());
		support.setRunStore(store);
		store.setPolicy(policy);
		support.setRunFactory(runFactory = new ExampleRun.Builder(1));
		support.setListenerFactory(lFactory = new SimpleListenerFactory());
		lFactory.setBuilders(singletonMap(
				"foo",
				(SimpleListenerFactory.Builder) new SimpleListenerFactory.Builder() {
					@Override
					public Listener build(TavernaRun run, String configuration)
							throws NoListenerException {
						return makeListener(run, configuration);
					}
				}));
	}

	@Test
	public void defaults1() {
		assertNotNull(server);
	}

	@Test
	public void defaults2() {
		assertEquals(10, server.getMaxSimultaneousRuns());
	}

	@Test
	public void defaults3() {
		assertEquals(1, server.getAllowedListeners().length);
	}

	@Test
	public void defaults4() {
		assertNotNull(support.getPrincipal());
	}

	@Test
	public void serverAsksPolicyForMaxRuns() {
		int oldmax = policy.maxruns;
		try {
			policy.maxruns = 1;
			assertEquals(1, server.getMaxSimultaneousRuns());
		} finally {
			policy.maxruns = oldmax;
		}
	}

	@Test
	public void makeAndKillARun() throws NoUpdateException, UnknownRunException {
		RunReference rr = server.submitWorkflow(null);
		assertNotNull(rr);
		assertNotNull(rr.name);
		server.destroyRun(rr.name);
	}

	@Test
	public void makeListenKillRun() throws Exception {
		RunReference run = server.submitWorkflow(null);
		try {
			lrunname = lrunconf = null;
			assertEquals(asList("foo"), asList(server.getAllowedListeners()));
			String l = server.addRunListener(run.name, "foo", "foobar");
			assertEquals("bar", l);
			assertEquals("foobar", lrunconf);
			assertEquals(lrunname, support.getRun(run.name).toString());
			assertEquals(asList("default", "bar"),
					asList(server.getRunListeners(run.name)));
			assertEquals(0,
					server.getRunListenerProperties(run.name, "bar").length);
		} finally {
			try {
				server.destroyRun(run.name);
			} catch (Exception e) {
				// Ignore
			}
		}
	}
}
