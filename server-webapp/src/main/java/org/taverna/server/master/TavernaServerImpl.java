package org.taverna.server.master;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.seeOther;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.ws.handler.MessageContext.PATH_INFO;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.DirEntryReference.newInstance;

import java.io.StringWriter;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.ws.rs.Path;
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
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.logging.Log;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.Namespaces;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.SCUFL;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.DirectoryContents;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry;
import org.taverna.server.master.rest.TavernaServerDirectoryREST;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry.MakeDirectory;
import org.taverna.server.master.rest.TavernaServerInputREST.InDesc.AbstractContents;
import org.taverna.server.master.rest.TavernaServerListenersREST.ListenerDescription;
import org.taverna.server.master.rest.TavernaServerListenersREST.TavernaServerListenerREST;
import org.taverna.server.master.soap.TavernaServerSOAP;

/**
 * The core implementation of the web application.
 * 
 * @author Donal Fellows
 */
@Path("/")
@WebService(endpointInterface = "org.taverna.server.master.soap.TavernaServerSOAP", serviceName = "TavernaServer", targetNamespace = Namespaces.SERVER_SOAP)
@ManagedResource(objectName = "Taverna:group=Server,name=Webapp", description = "The main web-application interface to Taverna Server.")
public class TavernaServerImpl implements TavernaServerSOAP, TavernaServerREST {
	/** The logger for the server framework. */
	public static Log log = getLog(TavernaServerImpl.class);

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// CONNECTIONS TO JMX, SPRING AND CXF

	static int invokes;
	private JAXBContext scuflSerializer;
	/**
	 * Whether outgoing exceptions should be logged before being converted to
	 * responses.
	 */
	public static boolean logOutgoingExceptions = false;

	/**
	 * @throws JAXBException
	 */
	public TavernaServerImpl() throws JAXBException {
		scuflSerializer = JAXBContext.newInstance(SCUFL.class);
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
		return runStore.listRuns(null, null).size();
	}

	/**
	 * @return Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public boolean getLogIncomingWorkflows() {
		return logIncomingWorkflows;
	}

	/**
	 * @param logIncomingWorkflows
	 *            Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		this.logIncomingWorkflows = logIncomingWorkflows;
	}

	/**
	 * @return Whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public boolean getLogOutgoingExceptions() {
		return logOutgoingExceptions;
	}

	/**
	 * @param logOutgoing
	 *            Whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public void setLogOutgoingExceptions(boolean logOutgoing) {
		logOutgoingExceptions = logOutgoing;
	}

	/** Whether we should log all workflows sent to us. */
	private boolean logIncomingWorkflows;

	/** Whether we allow the creation of new workflow runs. */
	private boolean allowNewWorkflowRuns = true;

	@Resource
	private WebServiceContext jaxwsContext;
	@Resource
	private SecurityContext jaxrsContext;

	/** Encapsulates the policies applied by this server. */
	Policy policy;
	/** A factory for workflow runs. */
	RunFactory runFactory;
	/** A storage facility for workflow runs. */
	RunStore runStore;
	/** A factory for event listeners to attach to workflow runs. */
	ListenerFactory listenerFactory;

	/**
	 * @param policy
	 *            The policy being installed by Spring.
	 */
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	/**
	 * @param listenerFactory
	 *            The listener factory being installed by Spring.
	 */
	public void setListenerFactory(ListenerFactory listenerFactory) {
		this.listenerFactory = listenerFactory;
	}

	/**
	 * @param runFactory
	 *            The run factory being installed by Spring.
	 */
	public void setRunFactory(RunFactory runFactory) {
		this.runFactory = runFactory;
	}

	/**
	 * @param runStore
	 *            The run store being installed by Spring.
	 */
	public void setRunStore(RunStore runStore) {
		this.runStore = runStore;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE

	@Override
	public ServerDescription describeService(UriInfo ui) {
		invokes++;
		return new ServerDescription(runStore.listRuns(getPrincipal(), policy),
				ui);
	}

	@Override
	public RunList listUsersRuns(UriInfo ui) {
		return new RunList(runStore.listRuns(getPrincipal(), policy), ui
				.getAbsolutePathBuilder().path("{name}"));
	}

	@Override
	public Response submitWorkflow(SCUFL workflow, UriInfo ui)
			throws NoUpdateException {
		invokes++;
		String name = buildWorkflow(workflow, getPrincipal());
		return seeOther(ui.getAbsolutePathBuilder().path("{uuid}").build(name))
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
	public PermittedWorkflows getPermittedWorkflows() {
		invokes++;
		return new PermittedWorkflows(policy
				.listPermittedWorkflows(getPrincipal()));
	}

	@Override
	public PermittedListeners getPermittedListeners() {
		invokes++;
		return new PermittedListeners(listenerFactory
				.getSupportedListenerTypes());
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
			public Response destroy(UriInfo ui) throws NoUpdateException {
				invokes++;
				policy.permitDestroy(getPrincipal(), run);
				runStore.unregisterRun(runName);
				run.destroy();
				return temporaryRedirect(
						ui.getBaseUriBuilder().path("runs").build()).build();
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
						policy.permitUpdate(getPrincipal(), run);
						String name = listenerFactory.makeListener(run,
								typeAndConfiguration.type,
								typeAndConfiguration.configuration).getName();
						return seeOther(
								ui.getAbsolutePathBuilder().path(
										"{listenerName}").build(name)).build();
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
						for (Listener l : run.getListeners())
							result.add(new ListenerDescription(l.getName(), l
									.getType(), l.listProperties(), ui));
						return new Listeners(result);
					}
				};
			}

			@Override
			public String getOwner() {
				invokes++;
				return run.getSecurityContext().getOwner().getName();
			}

			@Override
			public String getExpiry() {
				invokes++;
				return df().format(run.getExpiry());
			}

			@Override
			public String getStatus() {
				invokes++;
				return run.getStatus().toString();
			}

			@Override
			public SCUFL getWorkflow() {
				invokes++;
				return run.getWorkflow();
			}

			@Override
			public DirectoryREST getWorkingDirectory() {
				invokes++;
				return new DirectoryREST(run);
			}

			@Override
			public Response setExpiry(String expiry, UriInfo ui)
					throws NoUpdateException {
				invokes++;
				policy.permitDestroy(getPrincipal(), run);
				try {
					run.setExpiry(df().parse(expiry));
				} catch (ParseException e) {
					throw new NoUpdateException(e.getMessage(), e);
				}
				return seeOther(ui.getRequestUri()).build();
			}

			@Override
			public Response setStatus(String status, UriInfo ui)
					throws NoUpdateException {
				invokes++;
				policy.permitUpdate(getPrincipal(), run);
				run.setStatus(Status.valueOf(status.trim()));
				return seeOther(ui.getRequestUri()).build();
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
							throws BadPropertyValueException {
						invokes++;
						Input i = TavernaServerImpl.getInput(run, name);
						if (i == null)
							throw new BadPropertyValueException(
									"unknown input port name");
						return new InDesc(i);
					}

					@Override
					public Response setBaclavaFile(String filename, UriInfo ui)
							throws NoUpdateException, BadStateChangeException,
							FilesystemAccessException {
						invokes++;
						policy.permitUpdate(getPrincipal(), run);
						run.setInputBaclavaFile(filename);
						return seeOther(ui.getRequestUri()).build();
					}

					@Override
					public Response setInput(String name,
							InDesc inputDescriptor, UriInfo ui)
							throws NoUpdateException, BadStateChangeException,
							FilesystemAccessException,
							BadPropertyValueException {
						invokes++;
						AbstractContents ac = inputDescriptor.assignment;
						if (name == null || name.isEmpty())
							throw new BadPropertyValueException(
									"bad property name");
						if (ac == null)
							throw new BadPropertyValueException("no content!");
						if (!(ac instanceof InDesc.File || ac instanceof InDesc.Value))
							throw new BadPropertyValueException(
									"unknown content type");
						policy.permitUpdate(getPrincipal(), run);
						Input i = TavernaServerImpl.getInput(run, name);
						if (i == null)
							i = run.makeInput(name);
						if (ac instanceof InDesc.File)
							i.setFile(ac.contents);
						else
							i.setValue(ac.contents);
						return seeOther(ui.getRequestUri()).build();
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
			public Response setOutputFile(String filename, UriInfo ui)
					throws NoUpdateException, FilesystemAccessException,
					BadStateChangeException {
				invokes++;
				policy.permitUpdate(getPrincipal(), run);
				if (filename != null && filename.length() == 0)
					filename = null;
				run.setOutputBaclavaFile(filename);
				return seeOther(ui.getRequestUri()).build();
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
					return new ListenerDescription(listen.getName(), listen
							.getType(), listen.listProperties(), ui);
				}

				@Override
				public TavernaServerListenersREST.Properties getProperties(
						UriInfo ui) {
					invokes++;
					return new TavernaServerListenersREST.Properties(ui
							.getAbsolutePathBuilder().path(
									"../../{listener}/properties/{prop}"),
							listen.getName(), listen.listProperties());
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
							public Response setValue(String value, UriInfo ui)
									throws NoUpdateException,
									NoListenerException {
								invokes++;
								policy.permitUpdate(getPrincipal(), run);
								listen.setProperty(propertyName, value);
								return seeOther(ui.getRequestUri()).build();
							}
						};

					throw new NoListenerException("no such property");
				}
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
		public Response destroyDirectoryEntry(List<PathSegment> path, UriInfo ui)
				throws NoUpdateException, FilesystemAccessException {
			invokes++;
			policy.permitUpdate(getPrincipal(), run);
			DirectoryEntry entry = getDirEntry(run, path);
			entry.destroy();
			return temporaryRedirect(
					ui.getAbsolutePathBuilder().path("..").build()).build();
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
				UriInfo ui, Request req) throws FilesystemAccessException {
			invokes++;
			DirectoryEntry de = getDirEntry(run, path);

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
				return notAcceptable(variants).type(TEXT_PLAIN).entity(
						"Do not know what type of response to produce.")
						.build();

			// Produce the content to deliver up
			Object result;
			if (v.getMediaType().equals(APPLICATION_OCTET_STREAM_TYPE))
				// Only for files...
				result = ((File) de).getContents();
			else if (v.getMediaType().equals(APPLICATION_ZIP_TYPE))
				// Only for directories...
				result = ((Directory) de).getContentsAsZip();
			else
				// Only for directories...
				// XML or JSON; let CXF pick what to do
				result = new DirectoryContents(ui, ((Directory) de)
						.getContents());
			return ok(result).type(v.getMediaType()).build();
		}

		private boolean matchType(MediaType a, MediaType b) {
			log.info("comparing " + a.getType() + "/" + a.getSubtype()
					+ " and " + b.getType() + "/" + b.getSubtype());
			return (a.isWildcardType() || b.isWildcardType() || a.getType()
					.equals(b.getType()))
					&& (a.isWildcardSubtype() || b.isWildcardSubtype() || a
							.getSubtype().equals(b.getSubtype()));
		}

		@Override
		public Response getDirectoryOrFileContents(List<PathSegment> path,
				UriInfo ui, HttpHeaders headers)
				throws FilesystemAccessException {
			invokes++;
			DirectoryEntry de = getDirEntry(run, path);

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
				return notAcceptable(variants).type(TEXT_PLAIN).entity(
						"Do not know what type of response to produce.")
						.build();

			// Produce the content to deliver up
			Object result;
			if (wanted.equals(APPLICATION_OCTET_STREAM_TYPE))
				// Only for files...
				result = ((File) de).getContents();
			else if (wanted.equals(APPLICATION_ZIP_TYPE))
				// Only for directories...
				result = ((Directory) de).getContentsAsZip();
			else
				// Only for directories...
				// XML or JSON; let CXF pick what to do
				result = new DirectoryContents(ui, ((Directory) de)
						.getContents());
			return ok(result).type(wanted).build();
		}

		@Override
		public Response makeDirectoryOrUpdateFile(List<PathSegment> parent,
				MakeOrUpdateDirEntry op, UriInfo ui) throws NoUpdateException,
				FilesystemAccessException {
			invokes++;
			policy.permitUpdate(getPrincipal(), run);
			DirectoryEntry container = getDirEntry(run, parent);
			if (!(container instanceof Directory))
				throw new FilesystemAccessException(
						"You may not "
								+ ((op instanceof MakeDirectory) ? "make a subdirectory of"
										: "place a file in") + " a file.");
			if (op.name == null || op.name.length() == 0)
				throw new FilesystemAccessException("missing name attribute");
			Directory d = (Directory) container;
			UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");
			DirectoryEntry target;

			if (op instanceof MakeDirectory)
				target = d.makeSubdirectory(getPrincipal(), op.name);
			else {
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
				if (f == null)
					f = d.makeEmptyFile(getPrincipal(), op.name);
				f.setContents(op.contents);
				target = f;
			}

			return temporaryRedirect(ub.build(target.getName())).build();
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE

	@Override
	public RunReference[] listRuns() {
		invokes++;
		Principal p = getPrincipal();
		ArrayList<RunReference> ws = new ArrayList<RunReference>();
		UriBuilder ub = getRestfulRunReferenceBuilder();
		for (String runName : runStore.listRuns(p, policy).keySet())
			ws.add(new RunReference(runName, ub));
		return ws.toArray(new RunReference[ws.size()]);
	}

	@Override
	public RunReference submitWorkflow(SCUFL workflow) throws NoUpdateException {
		invokes++;
		String name = buildWorkflow(workflow, getPrincipal());
		return new RunReference(name, getRestfulRunReferenceBuilder());
	}

	@Override
	public SCUFL[] getAllowedWorkflows() {
		invokes++;
		return policy.listPermittedWorkflows(getPrincipal()).toArray(
				new SCUFL[0]);
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
		TavernaRun w = getRun(runName);
		policy.permitUpdate(getPrincipal(), w);
		runStore.unregisterRun(runName);
		w.destroy();
	}

	@Override
	public SCUFL getRunWorkflow(String runName) throws UnknownRunException {
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
		TavernaRun w = getRun(runName);
		policy.permitDestroy(getPrincipal(), w);
		w.setExpiry(d);
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
		policy.permitUpdate(getPrincipal(), w);
		w.setStatus(s);
	}

	@Override
	public String getRunOwner(String runName) throws UnknownRunException {
		invokes++;
		return getRun(runName).getSecurityContext().getOwner().getName();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Filesystem connection

	@Override
	public DirEntryReference[] getRunDirectoryContents(String runName,
			DirEntryReference d) throws UnknownRunException,
			FilesystemAccessException {
		invokes++;
		List<DirEntryReference> result = new ArrayList<DirEntryReference>();
		for (DirectoryEntry e : getDirectory(getRun(runName), d).getContents())
			result.add(newInstance(null, e));
		return result.toArray(new DirEntryReference[result.size()]);
	}

	@Override
	public byte[] getRunDirectoryAsZip(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException {
		invokes++;
		return getDirectory(getRun(runName), d).getContentsAsZip();
	}

	@Override
	public DirEntryReference makeRunDirectory(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException {
		invokes++;
		TavernaRun w = getRun(runName);
		Principal p = getPrincipal();
		policy.permitUpdate(p, w);
		Directory dir = getDirectory(w, parent).makeSubdirectory(p, name);
		return newInstance(null, dir);
	}

	@Override
	public DirEntryReference makeRunFile(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException {
		invokes++;
		TavernaRun w = getRun(runName);
		Principal p = getPrincipal();
		policy.permitUpdate(p, w);
		File f = getDirectory(w, parent).makeEmptyFile(p, name);
		return newInstance(null, f);
	}

	@Override
	public void destroyRunDirectoryEntry(String runName, DirEntryReference d)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException {
		invokes++;
		TavernaRun w = getRun(runName);
		policy.permitUpdate(getPrincipal(), w);
		getDirEntry(w, d).destroy();
	}

	@Override
	public byte[] getRunFileContents(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException {
		invokes++;
		return getFile(getRun(runName), d).getContents();
	}

	@Override
	public void setRunFileContents(String runName, DirEntryReference d,
			byte[] newContents) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException {
		invokes++;
		TavernaRun w = getRun(runName);
		policy.permitUpdate(getPrincipal(), w);
		getFile(w, d).setContents(newContents);
	}

	@Override
	public long getRunFileLength(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException {
		invokes++;
		return getFile(getRun(runName), d).getSize();
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
		TavernaRun w = getRun(runName);
		policy.permitUpdate(getPrincipal(), w);
		return listenerFactory.makeListener(w, listenerType, configuration)
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
		policy.permitUpdate(getPrincipal(), w);
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
		policy.permitUpdate(getPrincipal(), w);
		w.setInputBaclavaFile(fileName);
	}

	@Override
	public void setRunInputPortFile(String runName, String portName,
			String portFilename) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		invokes++;
		TavernaRun w = getRun(runName);
		policy.permitUpdate(getPrincipal(), w);
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
		policy.permitUpdate(getPrincipal(), w);
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
		policy.permitUpdate(getPrincipal(), w);
		w.setOutputBaclavaFile(outputFile);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SUPPORT METHODS

	private static DateFormat isoFormat;

	static DateFormat df() {
		if (isoFormat == null)
			isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		return isoFormat;
	}

	private String buildWorkflow(SCUFL workflow, Principal p)
			throws NoCreateException {
		if (!allowNewWorkflowRuns)
			throw new NoCreateException("run creation not currently enabled");
		if (logIncomingWorkflows)
			try {
				StringWriter sw = new StringWriter();
				scuflSerializer.createMarshaller().marshal(workflow, sw);
				log.info(sw);
			} catch (JAXBException e) {
				log.warn("problem when logging workflow", e);
			}
		policy.permitCreate(p, workflow);

		TavernaRun w;
		try {
			w = runFactory.create(p, workflow);
		} catch (Exception e) {
			log.error("failed to build workflow run worker", e);
			throw new NoCreateException("failed to build workflow run worker");
		}

		String uuid = randomUUID().toString();
		runStore.registerRun(uuid, w);
		return uuid;
	}

	private UriBuilder getRestfulRunReferenceBuilder() {
		if (jaxwsContext == null)
			// Hack to make the test suite work
			return fromUri("/taverna-server/rest/runs").path("{uuid}");
		MessageContext mc = jaxwsContext.getMessageContext();
		String pathInfo = (String) mc.get(PATH_INFO);
		return fromUri(pathInfo.replaceFirst("/soap$", "/rest/runs")).path(
				"{uuid}");
	}

	TavernaRun getRun(String runName) throws UnknownRunException {
		Principal p = getPrincipal();
		for (Map.Entry<String, TavernaRun> w : runStore.listRuns(p, policy)
				.entrySet())
			if (w.getKey().equals(runName))
				return w.getValue();
		throw new UnknownRunException();
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

	private Directory getDirectory(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException {
		DirectoryEntry dirEntry = getDirEntry(run, d);
		if (dirEntry instanceof Directory)
			return (Directory) dirEntry;
		throw new FilesystemAccessException("not a directory");
	}

	private File getFile(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException {
		DirectoryEntry dirEntry = getDirEntry(run, d);
		if (dirEntry instanceof File)
			return (File) dirEntry;
		throw new FilesystemAccessException("not a file");
	}

	private DirectoryEntry getDirEntry(TavernaRun run, DirEntryReference d)
			throws FilesystemAccessException {
		Directory dir = run.getWorkingDirectory();
		DirectoryEntry found = dir;
		for (String bit : d.path.split("/")) {
			found = null;
			if (dir == null)
				throw new FilesystemAccessException("no such directory entry");
			for (DirectoryEntry entry : dir.getContents()) {
				if (entry.getName().equals(bit)) {
					found = entry;
					break;
				}
			}
			if (found == null)
				throw new FilesystemAccessException("no such directory entry");
			if (found instanceof Directory) {
				dir = (Directory) found;
			} else {
				dir = null;
			}
		}
		return found;
	}

	DirectoryEntry getDirEntry(TavernaRun run, List<PathSegment> d)
			throws FilesystemAccessException {
		Directory dir = run.getWorkingDirectory();
		DirectoryEntry found = dir;
		for (PathSegment segment : d)
			for (String bit : segment.getPath().split("/")) {
				found = null;
				if (dir == null)
					throw new FilesystemAccessException(
							"no such directory entry");
				for (DirectoryEntry entry : dir.getContents()) {
					if (entry.getName().equals(bit)) {
						found = entry;
						break;
					}
				}
				if (found == null)
					throw new FilesystemAccessException(
							"no such directory entry");
				if (found instanceof Directory) {
					dir = (Directory) found;
				} else {
					dir = null;
				}
			}
		return found;
	}

	Principal getPrincipal() {
		try {
			if (jaxwsContext != null)
				return jaxwsContext.getUserPrincipal();
			if (jaxrsContext != null)
				return jaxrsContext.getUserPrincipal();
		} catch (NullPointerException e) {
		}
		return null;
	}
}
