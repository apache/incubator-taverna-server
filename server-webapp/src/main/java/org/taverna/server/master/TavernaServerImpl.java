/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.sort;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.seeOther;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.ws.handler.MessageContext.PATH_INFO;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.taverna.server.master.common.DirEntryReference.newInstance;
import static org.taverna.server.master.common.Status.Initialized;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceContext;

import org.apache.commons.logging.Log;
import org.apache.cxf.common.security.SimplePrincipal;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.Namespaces;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadInputPortNameException;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoCredentialException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.NotOwnerException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.DirectoryContents;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry.MakeDirectory;
import org.taverna.server.master.rest.TavernaServerSecurityREST;
import org.taverna.server.master.rest.TavernaServerDirectoryREST;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.rest.TavernaServerInputREST.InDesc.AbstractContents;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.rest.TavernaServerListenersREST.ListenerDescription;
import org.taverna.server.master.rest.TavernaServerListenersREST.TavernaServerListenerREST;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.soap.PermissionList;
import org.taverna.server.master.soap.TavernaServerSOAP;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.output_description.RdfWrapper;

/**
 * The core implementation of the web application.
 * 
 * @author Donal Fellows
 */
@Path("/")
@WebService(endpointInterface = "org.taverna.server.master.soap.TavernaServerSOAP", serviceName = "TavernaServer", targetNamespace = Namespaces.SERVER_SOAP)
@ManagedResource(objectName = TavernaServerImpl.JMX_ROOT + "Webapp", description = "The main web-application interface to Taverna Server.")
public class TavernaServerImpl implements TavernaServerSOAP, TavernaServerREST {
	public static final String JMX_ROOT = "Taverna:group=Server-v2,name=";
	/** The logger for the server framework. */
	public static Log log = getLog(TavernaServerImpl.class);

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// CONNECTIONS TO JMX, SPRING AND CXF

	/**
	 * Simple count of number of times the service has been invoked. Rate of
	 * increase is how busy this service is.
	 */
	static int invokes;
	/**
	 * The XML serialization engine for workflows.
	 */
	private JAXBContext workflowSerializer;
	/**
	 * Whether to log failures during principal retrieval. Should be normally on
	 * as it indicates a serious problem, but can be switched off for testing.
	 */
	private transient boolean logGetPrincipalFailures = true;

	/**
	 * @throws JAXBException
	 */
	public TavernaServerImpl() throws JAXBException {
		workflowSerializer = JAXBContext.newInstance(Workflow.class);
	}

	/**
	 * @return Count of the number of external calls into this webapp.
	 */
	@ManagedAttribute(description = "Count of the number of external calls into this webapp.")
	public int getInvocationCount() {
		return invokes;
	}

	/**
	 * @return Current number of runs.
	 */
	@ManagedAttribute(description = "Current number of runs.")
	public int getCurrentRunCount() {
		return runStore.listRuns(null, policy).size();
	}

	/**
	 * @return Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public boolean getLogIncomingWorkflows() {
		return stateModel.getLogIncomingWorkflows();
	}

	/**
	 * @param logIncomingWorkflows
	 *            Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		stateModel.setLogIncomingWorkflows(logIncomingWorkflows);
	}

	/**
	 * @return Whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public boolean getLogOutgoingExceptions() {
		return stateModel.getLogOutgoingExceptions();
	}

	/**
	 * @param logOutgoing
	 *            Whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public void setLogOutgoingExceptions(boolean logOutgoing) {
		stateModel.setLogOutgoingExceptions(logOutgoing);
	}

	/**
	 * @return Whether to permit any new workflow runs to be created.
	 */
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	public boolean getAllowNewWorkflowRuns() {
		return stateModel.getAllowNewWorkflowRuns();
	}

	/**
	 * @param allowNewWorkflowRuns
	 *            Whether to permit any new workflow runs to be created.
	 */
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
		stateModel.setAllowNewWorkflowRuns(allowNewWorkflowRuns);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// STATE VARIABLES AND SPRING SETTERS

	@Resource
	private WebServiceContext jaxwsContext;
	@Resource
	private SecurityContext jaxrsContext;
	@Resource
	private HttpHeaders jaxrsHeaders;
	@Resource
	@Context
	ServletContext servletContext;

	/** Encapsulates the policies applied by this server. */
	Policy policy;
	/** A factory for workflow runs. */
	private RunFactory runFactory;
	/** A storage facility for workflow runs. */
	private RunStore runStore;
	/** A factory for event listeners to attach to workflow runs. */
	ListenerFactory listenerFactory;
	/** Connection to the persistent state of this service. */
	private ManagementModel stateModel;
	/** How to map the user ID to who to run as. */
	private LocalIdentityMapper idMapper;
	transient ContentsDescriptorBuilder cdBuilder;
	transient FilenameUtils fileUtils;

	/**
	 * @param policy
	 *            The policy being installed by Spring.
	 */
	@Required
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	/**
	 * @param listenerFactory
	 *            The listener factory being installed by Spring.
	 */
	@Required
	public void setListenerFactory(ListenerFactory listenerFactory) {
		this.listenerFactory = listenerFactory;
	}

	/**
	 * @param runFactory
	 *            The run factory being installed by Spring.
	 */
	@Required
	public void setRunFactory(RunFactory runFactory) {
		this.runFactory = runFactory;
	}

	/**
	 * @param runStore
	 *            The run store being installed by Spring.
	 */
	@Required
	public void setRunStore(RunStore runStore) {
		this.runStore = runStore;
	}

	/**
	 * @param stateModel
	 *            The state model engine being installed by Spring.
	 */
	@Required
	public void setStateModel(ManagementModel stateModel) {
		this.stateModel = stateModel;
	}

	/**
	 * @param mapper
	 *            The identity mapper being installed by Spring.
	 */
	@Required
	public void setIdMapper(LocalIdentityMapper mapper) {
		this.idMapper = mapper;
	}

	/**
	 * @param converter
	 *            The filename converter being installed by Spring.
	 */
	@Required
	public void setFileUtils(FilenameUtils converter) {
		this.fileUtils = converter;
	}

	/**
	 * @param cdBuilder
	 *            The contents descriptor builder being installed by Spring.
	 */
	@Required
	public void setContentsDescriptorBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE

	@Override
	public ServerDescription describeService(UriInfo ui) {
		invokes++;
		return new ServerDescription(runs(), ui);
	}

	@Override
	public RunList listUsersRuns(UriInfo ui) {
		return new RunList(runs(), ui.getAbsolutePathBuilder().path("{name}"));
	}

	@Override
	public Response submitWorkflow(Workflow workflow, UriInfo ui)
			throws NoUpdateException {
		invokes++;
		String name = buildWorkflow(workflow, getPrincipal());
		return created(ui.getAbsolutePathBuilder().path("{uuid}").build(name))
				.build();
	}

	@Override
	public int getMaxSimultaneousRuns() {
		invokes++;
		Integer limit = policy.getMaxRuns(getPrincipal());
		if (limit == null)
			return policy.getMaxRuns();
		return min(limit.intValue(), policy.getMaxRuns());
	}

	@Override
	public PolicyView getPolicyDescription() {
		return new PolicyView() {
			@Override
			public PolicyDescription getDescription(UriInfo ui) {
				return new PolicyDescription(ui);
			}

			@Override
			public int getMaxSimultaneousRuns() {
				invokes++;
				Integer limit = policy.getMaxRuns(getPrincipal());
				if (limit == null)
					return policy.getMaxRuns();
				return min(limit.intValue(), policy.getMaxRuns());
			}

			@Override
			public PermittedListeners getPermittedListeners() {
				invokes++;
				return new PermittedListeners(
						listenerFactory.getSupportedListenerTypes());
			}

			@Override
			public PermittedWorkflows getPermittedWorkflows() {
				invokes++;
				return new PermittedWorkflows(
						policy.listPermittedWorkflows(getPrincipal()));
			}
		};
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE - Workflow run

	@Override
	public TavernaServerRunREST getRunResource(final String runName)
			throws UnknownRunException {
		invokes++;
		final TavernaRun run = getRun(runName);
		return new TavernaServerRunREST() {
			@Override
			public RunDescription getDescription(UriInfo ui) {
				invokes++;
				return new RunDescription(run, ui);
			}

			@Override
			public Response destroy() throws NoUpdateException {
				invokes++;
				try {
					unregisterRun(runName, run);
				} catch (UnknownRunException e) {
					log.fatal("can't happen", e);
				}
				return noContent().build();
			}

			@Override
			public TavernaServerListenersREST getListeners() {
				invokes++;
				return new TavernaServerListenersREST() {
					@Override
					public Response addListener(
							ListenerDefinition typeAndConfiguration, UriInfo ui)
							throws NoUpdateException, NoListenerException {
						invokes++;
						String name = makeListener(run,
								typeAndConfiguration.type,
								typeAndConfiguration.configuration).getName();
						return created(
								ui.getAbsolutePathBuilder()
										.path("{listenerName}").build(name))
								.build();
					}

					@Override
					public TavernaServerListenerREST getListener(String name)
							throws NoListenerException {
						invokes++;
						Listener l = TavernaServerImpl.getListener(run, name);
						if (l == null)
							throw new NoListenerException();
						return new ListenerIfcImpl(l);
					}

					@Override
					public Listeners getDescription(UriInfo ui) {
						List<ListenerDescription> result = new ArrayList<ListenerDescription>();
						invokes++;
						UriBuilder ub = ui.getAbsolutePathBuilder().path(
								"{name}");
						for (Listener l : run.getListeners()) {
							URI base = ub.build(l.getName());
							result.add(new ListenerDescription(l, fromUri(base)));
						}
						return new Listeners(result, ub);
					}
				};
			}

			@Override
			public TavernaServerSecurityREST getSecurity()
					throws NotOwnerException {
				invokes++;
				final TavernaSecurityContext context = run.getSecurityContext();
				if (!getPrincipal().equals(context.getOwner()))
					throw new NotOwnerException();

				return new TavernaServerSecurityREST() {
					@Override
					public Descriptor describe(UriInfo ui) {
						invokes++;
						return new Descriptor(ui.getAbsolutePathBuilder().path(
								"{element}"), context.getOwner().getName(),
								context.getCredentials(), context.getTrusted());
					}

					@Override
					public String getOwner() {
						invokes++;
						return context.getOwner().getName();
					}

					@Override
					public CredentialList listCredentials() {
						invokes++;
						return new CredentialList(context.getCredentials());
					}

					@Override
					public Credential getParticularCredential(String id)
							throws NoCredentialException {
						invokes++;
						for (Credential c : context.getCredentials())
							if (c.id.equals(id))
								return c;
						throw new NoCredentialException();
					}

					@Override
					public Credential setParticularCredential(String id,
							Credential c, UriInfo ui)
							throws InvalidCredentialException,
							BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						c.id = id;
						c.href = ui.getAbsolutePath().toString();
						context.validateCredential(c);
						context.deleteCredential(c);
						context.addCredential(c);
						return c;
					}

					@Override
					public Response addCredential(Credential c, UriInfo ui)
							throws InvalidCredentialException,
							BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						c.id = randomUUID().toString();
						URI uri = ui.getAbsolutePathBuilder().path("{id}")
								.build(c.id);
						c.href = uri.toString();
						context.validateCredential(c);
						context.addCredential(c);
						return created(uri).build();
					}

					@Override
					public Response deleteAllCredentials(UriInfo ui)
							throws BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						for (Credential c : context.getCredentials())
							context.deleteCredential(c);
						return noContent().build();
					}

					@Override
					public Response deleteCredential(String id, UriInfo ui)
							throws BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						Credential toDelete = new Credential() {
						};
						toDelete.id = id;
						context.deleteCredential(toDelete);
						return noContent().build();
					}

					@Override
					public TrustList listTrusted() {
						invokes++;
						return new TrustList(context.getTrusted());
					}

					@Override
					public Trust getParticularTrust(String id)
							throws NoCredentialException {
						invokes++;
						for (Trust t : context.getTrusted())
							if (t.id.equals(id))
								return t;
						throw new NoCredentialException();
					}

					@Override
					public Trust setParticularTrust(String id, Trust t,
							UriInfo ui) throws InvalidCredentialException,
							BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						t.id = id;
						t.href = ui.getAbsolutePath().toString();
						context.validateTrusted(t);
						context.deleteTrusted(t);
						context.addTrusted(t);
						return t;
					}

					@Override
					public Response addTrust(Trust t, UriInfo ui)
							throws InvalidCredentialException,
							BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						t.id = randomUUID().toString();
						URI uri = ui.getAbsolutePathBuilder().path("{id}")
								.build(t.id);
						t.href = uri.toString();
						context.validateTrusted(t);
						context.addTrusted(t);
						return created(uri).build();
					}

					@Override
					public Response deleteAllTrusts(UriInfo ui)
							throws BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						for (Trust t : context.getTrusted())
							context.deleteTrusted(t);
						return noContent().build();
					}

					@Override
					public Response deleteTrust(String id, UriInfo ui)
							throws BadStateChangeException {
						invokes++;
						if (run.getStatus() != Initialized)
							throw new BadStateChangeException();
						Trust toDelete = new Trust();
						toDelete.id = id;
						context.deleteTrusted(toDelete);
						return noContent().build();
					}

					@Override
					public PermissionsDescription describePermissions(UriInfo ui) {
						invokes++;
						Map<String, Permission> perm = new HashMap<String, Permission>();
						for (String u : context.getPermittedReaders())
							perm.put(u, Permission.Read);
						for (String u : context.getPermittedUpdaters())
							perm.put(u, Permission.Update);
						for (String u : context.getPermittedDestroyers())
							perm.put(u, Permission.Destroy);
						return new PermissionsDescription(ui
								.getAbsolutePathBuilder().path("{id}"), perm);
					}

					@Override
					public Permission describePermission(String id) {
						invokes++;
						return getPerm(context, id);
					}

					@Override
					public Permission setPermission(String id, Permission perm) {
						invokes++;
						setPerm(context, id, perm);
						return getPerm(context, id);
					}

					@Override
					public Response deletePermission(String id, UriInfo ui) {
						invokes++;
						setPerm(context, id, Permission.None);
						return noContent().build();
					}

					@Override
					public Response makePermission(PermissionDescription desc,
							UriInfo ui) {
						invokes++;
						setPerm(context, desc.userName, desc.permission);
						return created(
								ui.getAbsolutePathBuilder().path("{user}")
										.build(desc.userName)).build();
					}
				};
			}

			@Override
			public String getExpiryTime() {
				invokes++;
				return dateTime().print(new DateTime(run.getExpiry()));
			}

			@Override
			public String getCreateTime() {
				invokes++;
				return dateTime().print(
						new DateTime(run.getCreationTimestamp()));
			}

			@Override
			public String getFinishTime() {
				invokes++;
				Date f = run.getFinishTimestamp();
				return f == null ? "" : dateTime().print(new DateTime(f));
			}

			@Override
			public String getStartTime() {
				invokes++;
				Date f = run.getStartTimestamp();
				return f == null ? "" : dateTime().print(new DateTime(f));
			}

			@Override
			public String getStatus() {
				invokes++;
				return run.getStatus().toString();
			}

			@Override
			public Workflow getWorkflow() {
				invokes++;
				return run.getWorkflow();
			}

			@Override
			public DirectoryREST getWorkingDirectory() {
				invokes++;
				return new DirectoryREST(run);
			}

			@Override
			public String setExpiryTime(String expiry) throws NoUpdateException {
				invokes++;
				try {
					return dateTime().print(
							new DateTime(updateExpiry(run, dateTimeParser()
									.parseDateTime(expiry.trim()).toDate())));
				} catch (IllegalArgumentException e) {
					throw new NoUpdateException(e.getMessage(), e);
				}
			}

			@Override
			public String setStatus(String status) throws NoUpdateException {
				invokes++;
				permitUpdate(run);
				run.setStatus(Status.valueOf(status.trim()));
				return run.getStatus().toString();
			}

			@Override
			public TavernaServerInputREST getInputs() {
				invokes++;
				return new TavernaServerInputREST() {
					@Override
					public InputsDescriptor get(UriInfo ui) {
						invokes++;
						return new InputsDescriptor(ui, run);
					}

					@Override
					public String getBaclavaFile() {
						invokes++;
						String i = run.getInputBaclavaFile();
						return i == null ? "" : i;
					}

					@Override
					public InDesc getInput(String name)
							throws BadInputPortNameException {
						invokes++;
						Input i = TavernaServerImpl.getInput(run, name);
						if (i == null)
							throw new BadInputPortNameException(
									"unknown input port name");
						return new InDesc(i);
					}

					@Override
					public String setBaclavaFile(String filename)
							throws NoUpdateException, BadStateChangeException,
							FilesystemAccessException {
						invokes++;
						permitUpdate(run);
						run.setInputBaclavaFile(filename);
						String i = run.getInputBaclavaFile();
						return i == null ? "" : i;
					}

					@Override
					public InDesc setInput(String name, InDesc inputDescriptor)
							throws NoUpdateException, BadStateChangeException,
							FilesystemAccessException,
							BadInputPortNameException,
							BadPropertyValueException {
						invokes++;
						AbstractContents ac = inputDescriptor.assignment;
						if (name == null || name.isEmpty())
							throw new BadInputPortNameException(
									"bad input name");
						if (ac == null)
							throw new BadPropertyValueException("no content!");
						if (!(ac instanceof InDesc.File || ac instanceof InDesc.Value))
							throw new BadPropertyValueException(
									"unknown content type");
						permitUpdate(run);
						Input i = TavernaServerImpl.getInput(run, name);
						if (i == null)
							i = run.makeInput(name);
						if (ac instanceof InDesc.File)
							i.setFile(ac.contents);
						else
							i.setValue(ac.contents);
						return new InDesc(i);
					}
				};
			}

			@Override
			public String getOutputFile() {
				invokes++;
				String o = run.getOutputBaclavaFile();
				return o == null ? "" : o;
			}

			@Override
			public String setOutputFile(String filename)
					throws NoUpdateException, FilesystemAccessException,
					BadStateChangeException {
				invokes++;
				permitUpdate(run);
				if (filename != null && filename.length() == 0)
					filename = null;
				run.setOutputBaclavaFile(filename);
				String o = run.getOutputBaclavaFile();
				return o == null ? "" : o;
			}

			class ListenerIfcImpl implements TavernaServerListenerREST {
				Listener listen;

				ListenerIfcImpl(Listener l) {
					this.listen = l;
				}

				@Override
				public String getConfiguration() {
					invokes++;
					return listen.getConfiguration();
				}

				@Override
				public ListenerDescription getDescription(UriInfo ui) {
					invokes++;
					return new ListenerDescription(listen,
							ui.getAbsolutePathBuilder());
				}

				@Override
				public TavernaServerListenersREST.Properties getProperties(
						UriInfo ui) {
					invokes++;
					return new TavernaServerListenersREST.Properties(ui
							.getAbsolutePathBuilder().path("{prop}"),
							listen.listProperties());
				}

				@Override
				public TavernaServerListenersREST.Property getProperty(
						final String propertyName) throws NoListenerException {
					invokes++;
					List<String> p = asList(listen.listProperties());
					if (p.contains(propertyName))
						return new TavernaServerListenersREST.Property() {
							@Override
							public String getValue() {
								invokes++;
								try {
									return listen.getProperty(propertyName);
								} catch (NoListenerException e) {
									log.error(
											"unexpected exception; property \""
													+ propertyName
													+ "\" should exist", e);
									return null;
								}
							}

							@Override
							public String setValue(String value)
									throws NoUpdateException,
									NoListenerException {
								invokes++;
								permitUpdate(run);
								listen.setProperty(propertyName, value);
								return listen.getProperty(propertyName);
							}
						};

					throw new NoListenerException("no such property");
				}
			}

			@Override
			public RdfWrapper getOutputDescription(UriInfo ui)
					throws BadStateChangeException, FilesystemAccessException,
					NoDirectoryEntryException {
				invokes++;
				if (run.getStatus() == Initialized)
					throw new BadStateChangeException(
							"may not get output description in initial state");
				return cdBuilder.makeOutputDescriptor(run, ui);
			}
		};
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE - Filesystem connection

	/** "application/zip" */
	static final MediaType APPLICATION_ZIP_TYPE = new MediaType("application",
			"zip");
	static final List<Variant> directoryVariants = asList(new Variant(
			APPLICATION_XML_TYPE, null, null), new Variant(
			APPLICATION_JSON_TYPE, null, null), new Variant(
			APPLICATION_ZIP_TYPE, null, null));
	static final List<Variant> fileVariants = singletonList(new Variant(
			APPLICATION_OCTET_STREAM_TYPE, null, null));

	class DirectoryREST implements TavernaServerDirectoryREST {
		private TavernaRun run;

		DirectoryREST(TavernaRun w) {
			this.run = w;
		}

		@Override
		public Response destroyDirectoryEntry(List<PathSegment> path)
				throws NoUpdateException, FilesystemAccessException,
				NoDirectoryEntryException {
			invokes++;
			permitUpdate(run);
			fileUtils.getDirEntry(run, path).destroy();
			return noContent().build();
		}

		@Override
		public DirectoryContents getDescription(UriInfo ui)
				throws FilesystemAccessException {
			invokes++;
			return new DirectoryContents(ui, run.getWorkingDirectory()
					.getContents());
		}

		// Nasty! This can have several different responses...
		// @Override
		public Response getDirectoryOrFileContents(List<PathSegment> path,
				UriInfo ui, Request req) throws FilesystemAccessException,
				NoDirectoryEntryException {
			invokes++;
			DirectoryEntry de = fileUtils.getDirEntry(run, path);

			// How did the user want the result?
			List<Variant> variants;
			if (de instanceof File)
				variants = fileVariants;
			else if (de instanceof Directory)
				variants = directoryVariants;
			else
				throw new FilesystemAccessException("not a directory or file!");
			Variant v = req.selectVariant(variants);
			if (v == null)
				return notAcceptable(variants)
						.type(TEXT_PLAIN)
						.entity("Do not know what type of response to produce.")
						.build();

			// Produce the content to deliver up
			Object result;
			if (v.getMediaType().equals(APPLICATION_OCTET_STREAM_TYPE))
				// Only for files...
				result = de;
			else if (v.getMediaType().equals(APPLICATION_ZIP_TYPE))
				// Only for directories...
				result = ((Directory) de).getContentsAsZip();
			else
				// Only for directories...
				// XML or JSON; let CXF pick what to do
				result = new DirectoryContents(ui,
						((Directory) de).getContents());
			return ok(result).type(v.getMediaType()).build();
		}

		private boolean matchType(MediaType a, MediaType b) {
			log.debug("comparing " + a.getType() + "/" + a.getSubtype()
					+ " and " + b.getType() + "/" + b.getSubtype());
			return (a.isWildcardType() || b.isWildcardType() || a.getType()
					.equals(b.getType()))
					&& (a.isWildcardSubtype() || b.isWildcardSubtype() || a
							.getSubtype().equals(b.getSubtype()));
		}

		@Override
		public Response getDirectoryOrFileContents(List<PathSegment> path,
				UriInfo ui, HttpHeaders headers)
				throws FilesystemAccessException, NoDirectoryEntryException {
			invokes++;
			DirectoryEntry de = fileUtils.getDirEntry(run, path);

			// How did the user want the result?
			List<Variant> variants;
			if (de instanceof File)
				variants = fileVariants;
			else if (de instanceof Directory)
				variants = directoryVariants;
			else
				throw new FilesystemAccessException("not a directory or file!");
			MediaType wanted = null;
			log.info("wanted this " + headers.getAcceptableMediaTypes());
			// Manual content negotiation!!! Ugh!
			outer: for (MediaType mt : headers.getAcceptableMediaTypes()) {
				for (Variant v : variants) {
					if (matchType(mt, v.getMediaType())) {
						wanted = v.getMediaType();
						break outer;
					}
				}
			}
			if (wanted == null)
				return notAcceptable(variants)
						.type(TEXT_PLAIN)
						.entity("Do not know what type of response to produce.")
						.build();

			// Produce the content to deliver up
			Object result;
			if (wanted.equals(APPLICATION_OCTET_STREAM_TYPE))
				// Only for files...
				result = de;
			else if (wanted.equals(APPLICATION_ZIP_TYPE))
				// Only for directories...
				result = ((Directory) de).getContentsAsZip();
			else
				// Only for directories...
				// XML or JSON; let CXF pick what to do
				result = new DirectoryContents(ui,
						((Directory) de).getContents());
			return ok(result).type(wanted).build();
		}

		@Override
		public Response makeDirectoryOrUpdateFile(List<PathSegment> parent,
				MakeOrUpdateDirEntry op, UriInfo ui) throws NoUpdateException,
				FilesystemAccessException, NoDirectoryEntryException {
			invokes++;
			permitUpdate(run);
			DirectoryEntry container = fileUtils.getDirEntry(run, parent);
			if (!(container instanceof Directory))
				throw new FilesystemAccessException(
						"You may not "
								+ ((op instanceof MakeDirectory) ? "make a subdirectory of"
										: "place a file in") + " a file.");
			if (op.name == null || op.name.length() == 0)
				throw new FilesystemAccessException("missing name attribute");
			Directory d = (Directory) container;
			UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");

			// Make a directory in the context directory

			if (op instanceof MakeDirectory) {
				Directory target = d.makeSubdirectory(getPrincipal(), op.name);
				return created(ub.build(target.getName())).build();
			}

			// Make or set the contents of a file

			File f = null;
			for (DirectoryEntry e : d.getContents()) {
				if (e.getName().equals(op.name)) {
					if (e instanceof Directory)
						throw new FilesystemAccessException(
								"You may not overwrite a directory with a file.");
					f = (File) e;
					break;
				}
			}
			if (f == null) {
				f = d.makeEmptyFile(getPrincipal(), op.name);
				f.setContents(op.contents);
				return created(ub.build(f.getName())).build();
			}
			f.setContents(op.contents);
			return seeOther(ub.build(f.getName())).build();
		}

		@Override
		public Response setFileContents(List<PathSegment> filePath,
				String name, InputStream contents, UriInfo ui)
				throws NoDirectoryEntryException, NoUpdateException,
				FilesystemAccessException {
			invokes++;
			permitUpdate(run);
			Directory d;
			if (filePath != null && filePath.size() > 0) {
				DirectoryEntry e = fileUtils.getDirEntry(run, filePath);
				if (!(e instanceof Directory)) {
					throw new FilesystemAccessException(
							"Cannot create a file that is not in a directory.");
				}
				d = (Directory) e;
			} else {
				d = run.getWorkingDirectory();
			}

			File f = null;
			for (DirectoryEntry e : d.getContents()) {
				if (e.getName().equals(name)) {
					if (e instanceof File) {
						f = (File) e;
						break;
					}
					throw new FilesystemAccessException(
							"Cannot create a file that is not in a directory.");
				}
			}
			if (f == null)
				f = d.makeEmptyFile(getPrincipal(), name);

			try {
				byte[] buffer = new byte[65536];
				int len = contents.read(buffer);
				if (len >= 0) {
					if (len < buffer.length) {
						byte[] newBuf = new byte[len];
						System.arraycopy(buffer, 0, newBuf, 0, len);
						buffer = newBuf;
					}
					f.setContents(buffer);
					while (len == 65536) {
						len = contents.read(buffer);
						if (len < 1)
							break;
						if (len < buffer.length) {
							byte[] newBuf = new byte[len];
							System.arraycopy(buffer, 0, newBuf, 0, len);
							buffer = newBuf;
						}
						f.appendContents(buffer);
					}
				}
			} catch (IOException exn) {
				throw new FilesystemAccessException("failed to transfer bytes",
						exn);
			}
			return seeOther(ui.getAbsolutePath()).build();
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE

	@Override
	public RunReference[] listRuns() {
		invokes++;
		ArrayList<RunReference> ws = new ArrayList<RunReference>();
		UriBuilder ub = getRunUriBuilder();
		for (String runName : runs().keySet())
			ws.add(new RunReference(runName, ub));
		return ws.toArray(new RunReference[ws.size()]);
	}

	@Override
	public RunReference submitWorkflow(Workflow workflow)
			throws NoUpdateException {
		invokes++;
		String name = buildWorkflow(workflow, getPrincipal());
		return new RunReference(name, getRunUriBuilder());
	}

	private static final Workflow[] WORKFLOW_ARRAY_TYPE = new Workflow[0];

	@Override
	public Workflow[] getAllowedWorkflows() {
		invokes++;
		return policy.listPermittedWorkflows(getPrincipal()).toArray(
				WORKFLOW_ARRAY_TYPE);
	}

	@Override
	public String[] getAllowedListeners() {
		invokes++;
		return listenerFactory.getSupportedListenerTypes().toArray(
				new String[0]);
	}

	@Override
	public void destroyRun(String runName) throws UnknownRunException,
			NoUpdateException {
		invokes++;
		unregisterRun(runName, null);
	}

	@Override
	public Workflow getRunWorkflow(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getWorkflow();
	}

	@Override
	public Date getRunExpiry(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getExpiry();
	}

	@Override
	public void setRunExpiry(String runName, Date d)
			throws UnknownRunException, NoUpdateException {
		invokes++;
		updateExpiry(getRun(runName), d);
	}

	@Override
	public Date getRunCreationTime(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getCreationTimestamp();
	}

	@Override
	public Date getRunFinishTime(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getFinishTimestamp();
	}

	@Override
	public Date getRunStartTime(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getStartTimestamp();
	}

	@Override
	public Status getRunStatus(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getStatus();
	}

	@Override
	public void setRunStatus(String runName, Status s)
			throws UnknownRunException, NoUpdateException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setStatus(s);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Security

	@Override
	public String getRunOwner(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getSecurityContext().getOwner().getName();
	}

	/**
	 * Look up a security context, applying access control rules for access to
	 * the parts of the context that are only open to the owner.
	 * 
	 * @param runName
	 *            The name of the workflow run.
	 * @param initialOnly
	 *            Whether to check if we're in the initial state.
	 * @return The security context. Never <tt>null</tt>.
	 * @throws UnknownRunException
	 * @throws NotOwnerException
	 * @throws BadStateChangeException
	 */
	private TavernaSecurityContext getRunSecurityContext(String runName,
			boolean initialOnly) throws UnknownRunException, NotOwnerException,
			BadStateChangeException {
		TavernaRun run = getRun(runName);
		TavernaSecurityContext c = run.getSecurityContext();
		if (!c.getOwner().equals(getPrincipal()))
			throw new NotOwnerException();
		if (initialOnly && run.getStatus() != Initialized)
			throw new BadStateChangeException();
		return c;
	}

	@Override
	public Credential[] getRunCredentials(String runName)
			throws UnknownRunException, NotOwnerException {
		invokes++;
		try {
			return getRunSecurityContext(runName, false).getCredentials();
		} catch (BadStateChangeException e) {
			Error e2 = new Error("impossible");
			e2.initCause(e);
			throw e2;
		}
	}

	@Override
	public String setRunCredential(String runName, String credentialID,
			Credential credential) throws UnknownRunException,
			NotOwnerException, InvalidCredentialException,
			NoCredentialException, BadStateChangeException {
		invokes++;
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		if (credentialID == null || credentialID.isEmpty()) {
			credential.id = randomUUID().toString();
		} else {
			find: do {
				for (Credential t : c.getCredentials())
					if (t.id.equals(credentialID))
						break find;
				throw new NoCredentialException();
			} while (false);
			credential.id = credentialID;
		}
		URI uri = getRunUriBuilder().path("security/credentials/{credid}")
				.build(runName, credential.id);
		credential.href = uri.toString();
		c.validateCredential(credential);
		c.addCredential(credential);
		return credential.id;
	}

	@Override
	public void deleteRunCredential(String runName, String credentialID)
			throws UnknownRunException, NotOwnerException,
			NoCredentialException, BadStateChangeException {
		invokes++;
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		Credential toDelete = new Credential() {
		};
		toDelete.id = credentialID;
		c.deleteCredential(toDelete);
	}

	@Override
	public Trust[] getRunCertificates(String runName)
			throws UnknownRunException, NotOwnerException {
		invokes++;
		try {
			return getRunSecurityContext(runName, false).getTrusted();
		} catch (BadStateChangeException e) {
			Error e2 = new Error("impossible");
			e2.initCause(e);
			throw e2;
		}
	}

	@Override
	public String setRunCertificates(String runName, String certificateID,
			Trust certificate) throws UnknownRunException, NotOwnerException,
			InvalidCredentialException, NoCredentialException,
			BadStateChangeException {
		invokes++;
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		if (certificateID == null || certificateID.isEmpty()) {
			certificate.id = randomUUID().toString();
		} else {
			find: do {
				for (Trust t : c.getTrusted())
					if (t.id.equals(certificateID))
						break find;
				throw new NoCredentialException();
			} while (false);
			certificate.id = certificateID;
		}
		URI uri = getRunUriBuilder().path("security/trusts/{certid}").build(
				runName, certificate.id);
		certificate.href = uri.toString();
		c.validateTrusted(certificate);
		c.addTrusted(certificate);
		return certificate.id;
	}

	@Override
	public void deleteRunCertificates(String runName, String certificateID)
			throws UnknownRunException, NotOwnerException,
			NoCredentialException, BadStateChangeException {
		invokes++;
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		Trust toDelete = new Trust();
		toDelete.id = certificateID;
		c.deleteTrusted(toDelete);
	}

	@Override
	public PermissionList listRunPermissions(String runName)
			throws UnknownRunException, NotOwnerException {
		PermissionList pl = new PermissionList();
		pl.permission = new ArrayList<PermissionList.SinglePermissionMapping>();
		Map<String, Permission> perm = new HashMap<String, Permission>();
		try {
			TavernaSecurityContext context = getRunSecurityContext(runName,
					false);
			for (String u : context.getPermittedReaders())
				perm.put(u, Permission.Read);
			for (String u : context.getPermittedUpdaters())
				perm.put(u, Permission.Update);
			for (String u : context.getPermittedDestroyers())
				perm.put(u, Permission.Destroy);
		} catch (BadStateChangeException e) {
			log.error("unexpected error from internal API", e);
		}
		List<String> users = new ArrayList<String>(perm.keySet());
		sort(users);
		for (String user : users) {
			pl.permission.add(new PermissionList.SinglePermissionMapping(user,
					perm.get(user)));
		}
		return pl;
	}

	@Override
	public void setRunPermission(String runName, String userName,
			Permission permission) throws UnknownRunException,
			NotOwnerException {
		try {
			setPerm(getRunSecurityContext(runName, false), userName, permission);
		} catch (BadStateChangeException e) {
			log.error("unexpected error from internal API", e);
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Filesystem connection

	@Override
	public RdfWrapper getRunOutputDescription(String runName)
			throws UnknownRunException, BadStateChangeException,
			FilesystemAccessException, NoDirectoryEntryException {
		invokes++;
		TavernaRun run = getRun(runName);
		if (run.getStatus() == Initialized) {
			throw new BadStateChangeException(
					"may not get output description in initial state");
		}
		return cdBuilder.makeOutputDescriptor(run, null);
	}

	@Override
	public DirEntryReference[] getRunDirectoryContents(String runName,
			DirEntryReference d) throws UnknownRunException,
			FilesystemAccessException, NoDirectoryEntryException {
		invokes++;
		List<DirEntryReference> result = new ArrayList<DirEntryReference>();
		for (DirectoryEntry e : fileUtils.getDirectory(getRun(runName), d)
				.getContents())
			result.add(newInstance(null, e));
		return result.toArray(new DirEntryReference[result.size()]);
	}

	@Override
	public byte[] getRunDirectoryAsZip(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		invokes++;
		return fileUtils.getDirectory(getRun(runName), d).getContentsAsZip();
	}

	@Override
	public DirEntryReference makeRunDirectory(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		Directory dir = fileUtils.getDirectory(w, parent).makeSubdirectory(
				getPrincipal(), name);
		return newInstance(null, dir);
	}

	@Override
	public DirEntryReference makeRunFile(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		File f = fileUtils.getDirectory(w, parent).makeEmptyFile(
				getPrincipal(), name);
		return newInstance(null, f);
	}

	@Override
	public void destroyRunDirectoryEntry(String runName, DirEntryReference d)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		fileUtils.getDirEntry(w, d).destroy();
	}

	@Override
	public byte[] getRunFileContents(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		invokes++;
		File f = fileUtils.getFile(getRun(runName), d);
		return f.getContents(0, -1);
	}

	@Override
	public void setRunFileContents(String runName, DirEntryReference d,
			byte[] newContents) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		fileUtils.getFile(w, d).setContents(newContents);
	}

	@Override
	public long getRunFileLength(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		invokes++;
		return fileUtils.getFile(getRun(runName), d).getSize();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Run listeners

	@Override
	public String[] getRunListeners(String runName) throws UnknownRunException {
		invokes++;
		TavernaRun w = getRun(runName);
		List<String> result = new ArrayList<String>();
		for (Listener l : w.getListeners())
			result.add(l.getName());
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String addRunListener(String runName, String listenerType,
			String configuration) throws UnknownRunException,
			NoUpdateException, NoListenerException {
		invokes++;
		return makeListener(getRun(runName), listenerType, configuration)
				.getName();
	}

	@Override
	public String getRunListenerConfiguration(String runName,
			String listenerName) throws UnknownRunException,
			NoListenerException {
		invokes++;
		return getListener(runName, listenerName).getConfiguration();
	}

	@Override
	public String[] getRunListenerProperties(String runName, String listenerName)
			throws UnknownRunException, NoListenerException {
		invokes++;
		return getListener(runName, listenerName).listProperties().clone();
	}

	@Override
	public String getRunListenerProperty(String runName, String listenerName,
			String propName) throws UnknownRunException, NoListenerException {
		invokes++;
		return getListener(runName, listenerName).getProperty(propName);
	}

	@Override
	public void setRunListenerProperty(String runName, String listenerName,
			String propName, String value) throws UnknownRunException,
			NoUpdateException, NoListenerException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		Listener l = getListener(w, listenerName);
		try {
			l.getProperty(propName); // sanity check!
			l.setProperty(propName, value);
		} catch (RuntimeException e) {
			throw new NoListenerException("problem setting property: "
					+ e.getMessage(), e);
		}
	}

	@Override
	public InputDescription getRunInputs(String runName)
			throws UnknownRunException {
		invokes++;
		return new InputDescription(getRun(runName));
	}

	@Override
	public String getRunOutputBaclavaFile(String runName)
			throws UnknownRunException {
		invokes++;
		return getRun(runName).getOutputBaclavaFile();
	}

	@Override
	public void setRunInputBaclavaFile(String runName, String fileName)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setInputBaclavaFile(fileName);
	}

	@Override
	public void setRunInputPortFile(String runName, String portName,
			String portFilename) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		Input i = getInput(w, portName);
		if (i == null)
			i = w.makeInput(portName);
		i.setFile(portFilename);
	}

	@Override
	public void setRunInputPortValue(String runName, String portName,
			String portValue) throws UnknownRunException, NoUpdateException,
			BadStateChangeException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		Input i = getInput(w, portName);
		if (i == null)
			i = w.makeInput(portName);
		i.setValue(portValue);
	}

	@Override
	public void setRunOutputBaclavaFile(String runName, String outputFile)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		invokes++;
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setOutputBaclavaFile(outputFile);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SUPPORT METHODS

	private String buildWorkflow(Workflow workflow, Principal p)
			throws NoCreateException {
		if (!stateModel.getAllowNewWorkflowRuns())
			throw new NoCreateException("run creation not currently enabled");
		try {
			if (stateModel.getLogIncomingWorkflows()) {
				StringWriter sw = new StringWriter();
				workflowSerializer.createMarshaller().marshal(workflow, sw);
				log.info(sw);
			}
		} catch (JAXBException e) {
			log.warn("problem when logging workflow", e);
		}

		// Security checks
		policy.permitCreate(p, workflow);
		if (idMapper != null && idMapper.getUsernameForPrincipal(p) == null) {
			log.error("cannot map principal to local user id");
			throw new NoCreateException(
					"failed to map security token to local user id");
		}

		TavernaRun run;
		try {
			run = runFactory.create(p, workflow);
			TavernaSecurityContext c = run.getSecurityContext();
			if (servletContext != null)
				c.initializeSecurityFromContext(servletContext);
			if (jaxwsContext != null && jaxwsContext.getUserPrincipal() != null)
				c.initializeSecurityFromSOAPContext(jaxwsContext
						.getMessageContext());
			else if (jaxrsHeaders != null)
				c.initializeSecurityFromRESTContext(jaxrsHeaders);
		} catch (Exception e) {
			log.error("failed to build workflow run worker", e);
			throw new NoCreateException("failed to build workflow run worker");
		}

		return runStore.registerRun(run);
	}

	UriBuilder getRunUriBuilder() {
		if (jaxwsContext == null || jaxwsContext.getMessageContext() == null)
			// Hack to make the test suite work
			return fromUri("/taverna-server/rest/runs").path("{uuid}");
		String pathInfo = (String) jaxwsContext.getMessageContext().get(
				PATH_INFO);
		return fromUri(pathInfo.replaceFirst("/soap$", "/rest/runs")).path(
				"{uuid}");
	}

	UriBuilder getRunUriBuilder(TavernaRun run) {
		return fromUri(getRunUriBuilder().build(run.getID()));
	}

	Map<String, TavernaRun> runs() {
		return runStore.listRuns(getPrincipal(), policy);
	}

	TavernaRun getRun(String runName) throws UnknownRunException {
		if (isSuperUser())
			return runStore.getRun(runName);
		return runStore.getRun(getPrincipal(), policy, runName);
	}

	void unregisterRun(String runName, TavernaRun run)
			throws NoDestroyException, UnknownRunException {
		if (run == null)
			run = getRun(runName);
		policy.permitDestroy(getPrincipal(), run);
		runStore.unregisterRun(runName);
		run.destroy();
	}

	Date updateExpiry(TavernaRun run, Date target) throws NoDestroyException {
		policy.permitDestroy(getPrincipal(), run);
		run.setExpiry(target);
		return run.getExpiry();
	}

	static Input getInput(TavernaRun run, String portName) {
		for (Input i : run.getInputs())
			if (i.getName().equals(portName))
				return i;
		return null;
	}

	private Listener getListener(String runName, String listenerName)
			throws NoListenerException, UnknownRunException {
		return getListener(getRun(runName), listenerName);
	}

	static Listener getListener(TavernaRun run, String listenerName)
			throws NoListenerException {
		for (Listener l : run.getListeners())
			if (l.getName().equals(listenerName))
				return l;
		throw new NoListenerException();
	}

	Listener makeListener(TavernaRun run, String type, String config)
			throws NoListenerException, NoUpdateException {
		permitUpdate(run);
		return listenerFactory.makeListener(run, type, config);
	}

	/**
	 * Gets the identity of the user currently accessing the webapp, which is
	 * stored in a thread-safe way in the webapp's container's context.
	 * 
	 * @return The identity of the user accessing the webapp.
	 */
	Principal getPrincipal() {
		Principal p = null;
		if (jaxwsContext == null && jaxrsContext == null
				&& logGetPrincipalFailures)
			log.warn("no injected context");
		try {
			if (jaxwsContext != null
					&& jaxwsContext.getMessageContext() != null)
				p = jaxwsContext.getUserPrincipal();
		} catch (NullPointerException e) {
			if (logGetPrincipalFailures)
				log.warn("failed to get user principal", e);
		}
		if (p == null)
			try {
				if (jaxrsContext != null)
					p = jaxrsContext.getUserPrincipal();
			} catch (NullPointerException e) {
				if (logGetPrincipalFailures)
					log.warn("failed to get user principal", e);
			}
		if (p != null)
			log.info("service being accessed by " + p.getName());
		else {
			if (logGetPrincipalFailures)
				log.info("service being accessed by <NOBODY>");
			p = new SimplePrincipal("<NOBODY>");
		}
		return p;
	}

	/** The name of the role of the super-user. */
	public static final String SUPER_ROLE = "tavernasuperuser";

	private boolean isSuperUser() {
		if (jaxwsContext == null && jaxrsContext == null)
			return false;
		try {
			if (jaxwsContext != null)
				return jaxwsContext.isUserInRole(SUPER_ROLE);
		} catch (NullPointerException e) {
			if (logGetPrincipalFailures)
				log.warn("failed to get user principal via JAX-WS", e);
		}
		try {
			if (jaxrsContext != null)
				return jaxrsContext.isUserInRole(SUPER_ROLE);
		} catch (NullPointerException e) {
			if (logGetPrincipalFailures)
				log.warn("failed to get user principal via JAX-RS", e);
		}
		return false;
	}

	void permitUpdate(TavernaRun run) throws NoUpdateException {
		if (isSuperUser())
			return; // Superusers are fully authorized to access others things
		policy.permitUpdate(getPrincipal(), run);
	}

	void permitDestroy(TavernaRun run) throws NoUpdateException {
		if (isSuperUser())
			return; // Superusers are fully authorized to access others things
		policy.permitDestroy(getPrincipal(), run);
	}

	public void setLogGetPrincipalFailures(boolean logthem) {
		logGetPrincipalFailures = logthem;
	}

	void setPerm(TavernaSecurityContext context, String id, Permission perm) {
		Set<String> permSet;
		boolean doRead = false, doWrite = false, doKill = false;

		switch (perm) {
		case Destroy:
			doKill = true;
		case Update:
			doWrite = true;
		case Read:
			doRead = true;
		}

		permSet = context.getPermittedReaders();
		if (doRead)
			permSet.add(id);
		else
			permSet.remove(id);
		context.setPermittedReaders(permSet);

		permSet = context.getPermittedUpdaters();
		if (doWrite)
			permSet.add(id);
		else
			permSet.remove(id);
		context.setPermittedUpdaters(permSet);

		permSet = context.getPermittedDestroyers();
		if (doKill)
			permSet.add(id);
		else
			permSet.remove(id);
		context.setPermittedDestroyers(permSet);
	}

	Permission getPerm(TavernaSecurityContext context, String id) {
		if (context.getPermittedDestroyers().contains(id))
			return Permission.Destroy;
		if (context.getPermittedUpdaters().contains(id))
			return Permission.Update;
		if (context.getPermittedReaders().contains(id))
			return Permission.Read;
		return Permission.None;
	}
}
