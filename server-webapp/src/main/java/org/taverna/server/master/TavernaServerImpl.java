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
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.ws.handler.MessageContext.PATH_INFO;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.DirEntryReference.newInstance;
import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.common.Status.Initialized;

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
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.taverna.server.master.TavernaServerImpl.WebappAware;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.Namespaces;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
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
import org.taverna.server.master.notification.NotificationEngine;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.rest.TavernaServerREST.EnabledNotificationFabrics;
import org.taverna.server.master.rest.TavernaServerREST.PermittedListeners;
import org.taverna.server.master.rest.TavernaServerREST.PermittedWorkflows;
import org.taverna.server.master.rest.TavernaServerREST.PolicyView;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.soap.PermissionList;
import org.taverna.server.master.soap.TavernaServerSOAP;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.InvocationCounter;
import org.taverna.server.output_description.RdfWrapper;

/**
 * The core implementation of the web application.
 * 
 * @author Donal Fellows
 */
@Path("/")
@DeclareRoles({ USER, ADMIN })
@WebService(endpointInterface = "org.taverna.server.master.soap.TavernaServerSOAP", serviceName = "TavernaServer", targetNamespace = Namespaces.SERVER_SOAP)
@ManagedResource(objectName = TavernaServerImpl.JMX_ROOT + "Webapp", description = "The main web-application interface to Taverna Server.")
public abstract class TavernaServerImpl implements TavernaServerSOAP,
		TavernaServerREST, TavernaServer {
	public static final String JMX_ROOT = "Taverna:group=Server-v2,name=";
	/** The logger for the server framework. */
	public static Log log = getLog(TavernaServerImpl.class);

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// CONNECTIONS TO JMX, SPRING AND CXF

	/**
	 * The XML serialization engine for workflows.
	 */
	private JAXBContext workflowSerializer;
	/**
	 * Whether to log failures during principal retrieval. Should be normally on
	 * as it indicates a serious problem, but can be switched off for testing.
	 */
	private boolean logGetPrincipalFailures = true;

	/**
	 * @throws JAXBException
	 */
	public TavernaServerImpl() throws JAXBException {
		workflowSerializer = JAXBContext.newInstance(Workflow.class);
	}

	@Override
	@ManagedAttribute(description = "Count of the number of external calls into this webapp.")
	public int getInvocationCount() {
		return counter.getCount();
	}

	@Override
	@ManagedAttribute(description = "Current number of runs.")
	public int getCurrentRunCount() {
		return runStore.listRuns(null, policy).size();
	}

	@Override
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public boolean getLogIncomingWorkflows() {
		return stateModel.getLogIncomingWorkflows();
	}

	@Override
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		stateModel.setLogIncomingWorkflows(logIncomingWorkflows);
	}

	@Override
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public boolean getLogOutgoingExceptions() {
		return stateModel.getLogOutgoingExceptions();
	}

	@Override
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	public void setLogOutgoingExceptions(boolean logOutgoing) {
		stateModel.setLogOutgoingExceptions(logOutgoing);
	}

	@Override
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	public boolean getAllowNewWorkflowRuns() {
		return stateModel.getAllowNewWorkflowRuns();
	}

	@Override
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
		stateModel.setAllowNewWorkflowRuns(allowNewWorkflowRuns);
	}

	@Resource
	WebServiceContext jaxws;
	@Context
	SecurityContext jaxrsSecurity;
	@Context
	HttpHeaders jaxrsHeaders;
	@Resource
	private ServletContext context;

	@Override
	public void setServletContext(ServletContext context) {
		this.context = context;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// STATE VARIABLES AND SPRING SETTERS

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
	private InvocationCounter counter;
	/**
	 * For building descriptions of the expected inputs and actual outputs of a
	 * workflow.
	 */
	private ContentsDescriptorBuilder cdBuilder;
	/**
	 * Utilities for accessing files on the local-worker.
	 */
	private FilenameUtils fileUtils;
	private NotificationEngine notificationEngine;

	@Override
	@Required
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	@Override
	@Required
	public void setListenerFactory(ListenerFactory listenerFactory) {
		this.listenerFactory = listenerFactory;
	}

	@Override
	@Required
	public void setRunFactory(RunFactory runFactory) {
		this.runFactory = runFactory;
	}

	@Override
	@Required
	public void setRunStore(RunStore runStore) {
		this.runStore = runStore;
	}

	@Override
	@Required
	public void setStateModel(ManagementModel stateModel) {
		this.stateModel = stateModel;
	}

	@Override
	@Required
	public void setIdMapper(LocalIdentityMapper mapper) {
		this.idMapper = mapper;
	}

	@Override
	@Required
	public void setFileUtils(FilenameUtils converter) {
		this.fileUtils = converter;
	}

	@Override
	@Required
	public void setContentsDescriptorBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	@Override
	@Required
	public void setInvocationCounter(InvocationCounter counter) {
		this.counter = counter;
	}

	@Override
	@Required
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}
	
	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE

	@Override
	public ServerDescription describeService(UriInfo ui) {
		return new ServerDescription(ui);
	}

	@Override
	public RunList listUsersRuns(UriInfo ui) {
		return new RunList(runs(), ui.getAbsolutePathBuilder().path("{name}"));
	}

	@Override
	public Response submitWorkflow(Workflow workflow, UriInfo ui)
			throws NoUpdateException {
		String name = buildWorkflow(workflow, getPrincipal());
		return created(ui.getAbsolutePathBuilder().path("{uuid}").build(name))
				.build();
	}

	@Override
	public int getMaxSimultaneousRuns() {
		Integer limit = policy.getMaxRuns(getPrincipal());
		if (limit == null)
			return policy.getMaxRuns();
		return min(limit.intValue(), policy.getMaxRuns());
	}

	@Override
	public TavernaServerRunREST getRunResource(final String runName)
			throws UnknownRunException {
		RunREST rr = makeRunInterface();
		rr.setRun(getRun(runName));
		rr.setRunName(runName);
		return rr;
	}

	@Override
	public abstract PolicyView getPolicyDescription();

	/** Construct a RESTful interface to a run. */
	protected abstract RunREST makeRunInterface();

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

	/**
	 * Indicates that this is a class that wants to be told by Spring about the
	 * main webapp.
	 * 
	 * @author Donal Fellows
	 */
	interface WebappAware {
		@Required
		void setWebapp(TavernaServer webapp);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE

	@Override
	public RunReference[] listRuns() {
		ArrayList<RunReference> ws = new ArrayList<RunReference>();
		UriBuilder ub = getRunUriBuilder();
		for (String runName : runs().keySet())
			ws.add(new RunReference(runName, ub));
		return ws.toArray(new RunReference[ws.size()]);
	}

	@Override
	public RunReference submitWorkflow(Workflow workflow)
			throws NoUpdateException {
		String name = buildWorkflow(workflow, getPrincipal());
		return new RunReference(name, getRunUriBuilder());
	}

	private static final Workflow[] WORKFLOW_ARRAY_TYPE = new Workflow[0];

	@Override
	public Workflow[] getAllowedWorkflows() {
		return policy.listPermittedWorkflows(getPrincipal()).toArray(
				WORKFLOW_ARRAY_TYPE);
	}

	private static final String[] STRING_ARRAY_TYPE = new String[0];

	@Override
	public String[] getAllowedListeners() {
		return listenerFactory.getSupportedListenerTypes().toArray(
				STRING_ARRAY_TYPE);
	}

	@Override
	public String[] getEnabledNotifiers() {
		return notificationEngine.listAvailableDispatchers().toArray(
				STRING_ARRAY_TYPE);
	}

	@Override
	public void destroyRun(String runName) throws UnknownRunException,
			NoUpdateException {
		unregisterRun(runName, null);
	}

	@Override
	public Workflow getRunWorkflow(String runName) throws UnknownRunException {
		return getRun(runName).getWorkflow();
	}

	@Override
	public Date getRunExpiry(String runName) throws UnknownRunException {
		return getRun(runName).getExpiry();
	}

	@Override
	public void setRunExpiry(String runName, Date d)
			throws UnknownRunException, NoUpdateException {
		updateExpiry(getRun(runName), d);
	}

	@Override
	public Date getRunCreationTime(String runName) throws UnknownRunException {
		return getRun(runName).getCreationTimestamp();
	}

	@Override
	public Date getRunFinishTime(String runName) throws UnknownRunException {
		return getRun(runName).getFinishTimestamp();
	}

	@Override
	public Date getRunStartTime(String runName) throws UnknownRunException {
		return getRun(runName).getStartTimestamp();
	}

	@Override
	public Status getRunStatus(String runName) throws UnknownRunException {
		return getRun(runName).getStatus();
	}

	@Override
	public void setRunStatus(String runName, Status s)
			throws UnknownRunException, NoUpdateException {
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setStatus(s);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Security

	@Override
	public String getRunOwner(String runName) throws UnknownRunException {
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
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		Credential toDelete = new Credential() {
		};
		toDelete.id = credentialID;
		c.deleteCredential(toDelete);
	}

	@Override
	public Trust[] getRunCertificates(String runName)
			throws UnknownRunException, NotOwnerException {
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
			setPermission(getRunSecurityContext(runName, false), userName,
					permission);
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
		return fileUtils.getDirectory(getRun(runName), d).getContentsAsZip();
	}

	@Override
	public DirEntryReference makeRunDirectory(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
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
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		fileUtils.getDirEntry(w, d).destroy();
	}

	@Override
	public byte[] getRunFileContents(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		File f = fileUtils.getFile(getRun(runName), d);
		return f.getContents(0, -1);
	}

	@Override
	public void setRunFileContents(String runName, DirEntryReference d,
			byte[] newContents) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		fileUtils.getFile(w, d).setContents(newContents);
	}

	@Override
	public long getRunFileLength(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		return fileUtils.getFile(getRun(runName), d).getSize();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Run listeners

	@Override
	public String[] getRunListeners(String runName) throws UnknownRunException {
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
		return makeListener(getRun(runName), listenerType, configuration)
				.getName();
	}

	@Override
	public String getRunListenerConfiguration(String runName,
			String listenerName) throws UnknownRunException,
			NoListenerException {
		return getListener(runName, listenerName).getConfiguration();
	}

	@Override
	public String[] getRunListenerProperties(String runName, String listenerName)
			throws UnknownRunException, NoListenerException {
		return getListener(runName, listenerName).listProperties().clone();
	}

	@Override
	public String getRunListenerProperty(String runName, String listenerName,
			String propName) throws UnknownRunException, NoListenerException {
		return getListener(runName, listenerName).getProperty(propName);
	}

	@Override
	public void setRunListenerProperty(String runName, String listenerName,
			String propName, String value) throws UnknownRunException,
			NoUpdateException, NoListenerException {
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
		return new InputDescription(getRun(runName));
	}

	@Override
	public String getRunOutputBaclavaFile(String runName)
			throws UnknownRunException {
		return getRun(runName).getOutputBaclavaFile();
	}

	@Override
	public void setRunInputBaclavaFile(String runName, String fileName)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setInputBaclavaFile(fileName);
	}

	@Override
	public void setRunInputPortFile(String runName, String portName,
			String portFilename) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
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
		TavernaRun w = getRun(runName);
		permitUpdate(w);
		w.setOutputBaclavaFile(outputFile);
	}

	@Override
	public org.taverna.server.input_description.InputDescription getRunInputDescriptor(
			String runName) throws UnknownRunException {
		TavernaRun run = getRun(runName);
		return cdBuilder.makeInputDescriptor(run,
				getRunUriBuilder(run).path("inputs"));
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
			if (context != null)
				c.initializeSecurityFromContext(context);
			if (jaxws != null && jaxws.getUserPrincipal() != null)
				c.initializeSecurityFromSOAPContext(jaxws.getMessageContext());
			else if (jaxrsHeaders != null)
				c.initializeSecurityFromRESTContext(jaxrsHeaders);
		} catch (Exception e) {
			log.error("failed to build workflow run worker", e);
			throw new NoCreateException("failed to build workflow run worker");
		}

		return runStore.registerRun(run);
	}

	UriBuilder getRunUriBuilder() {
		if (jaxws == null || jaxws.getMessageContext() == null)
			// Hack to make the test suite work
			return fromUri("/taverna-server/rest/runs").path("{uuid}");
		String pathInfo = (String) jaxws.getMessageContext().get(PATH_INFO);
		return fromUri(pathInfo.replaceFirst("/soap$", "/rest/runs")).path(
				"{uuid}");
	}

	@Override
	public UriBuilder getRunUriBuilder(TavernaRun run) {
		return fromUri(getRunUriBuilder().build(run.getID()));
	}

	Map<String, TavernaRun> runs() {
		return runStore.listRuns(getPrincipal(), policy);
	}

	@Override
	public TavernaRun getRun(String runName) throws UnknownRunException {
		if (isSuperUser())
			return runStore.getRun(runName);
		return runStore.getRun(getPrincipal(), policy, runName);
	}

	@Override
	public void unregisterRun(String runName, TavernaRun run)
			throws NoDestroyException, UnknownRunException {
		if (run == null)
			run = getRun(runName);
		policy.permitDestroy(getPrincipal(), run);
		runStore.unregisterRun(runName);
		run.destroy();
	}

	@Override
	public Date updateExpiry(TavernaRun run, Date target)
			throws NoDestroyException {
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

	@Override
	public Listener makeListener(TavernaRun run, String type, String config)
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
	@Override
	public Principal getPrincipal() {
		try {
			Authentication auth = SecurityContextHolder.getContext()
					.getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				if (logGetPrincipalFailures)
					log.warn("failed to get auth; going with <NOBODY>");
				return new SimplePrincipal("<NOBODY>");
			}
			Object principal = auth.getPrincipal();
			if (principal instanceof Principal)
				return (Principal) principal;
			String username;
			if (principal instanceof UserDetails)
				username = ((UserDetails) principal).getUsername();
			else
				username = principal.toString();
			return new SimplePrincipal(username);
		} catch (RuntimeException e) {
			if (logGetPrincipalFailures)
				log.info("failed to map principal", e);
			throw e;
		}
	}

	private boolean isSuperUser() {
		try {
			adminAuthorizedThunk();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@RolesAllowed(ADMIN)
	protected final void adminAuthorizedThunk() {
	}

	@Override
	public void permitUpdate(TavernaRun run) throws NoUpdateException {
		if (isSuperUser())
			return; // Superusers are fully authorized to access others things
		policy.permitUpdate(getPrincipal(), run);
	}

	void permitDestroy(TavernaRun run) throws NoUpdateException {
		if (isSuperUser())
			return; // Superusers are fully authorized to access others things
		policy.permitDestroy(getPrincipal(), run);
	}

	@Override
	public void setLogGetPrincipalFailures(boolean logthem) {
		logGetPrincipalFailures = logthem;
	}

	@Override
	public void setPermission(TavernaSecurityContext context, String id,
			Permission perm) {
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

	@Override
	public Permission getPermission(TavernaSecurityContext context, String id) {
		if (context.getPermittedDestroyers().contains(id))
			return Permission.Destroy;
		if (context.getPermittedUpdaters().contains(id))
			return Permission.Update;
		if (context.getPermittedReaders().contains(id))
			return Permission.Read;
		return Permission.None;
	}
}

/**
 * RESTful interface to the policies of a Taverna Server installation.
 * 
 * @author Donal Fellows
 */
class PolicyREST implements PolicyView, WebappAware {
	private TavernaServer webapp;
	private Policy policy;
	private ListenerFactory listenerFactory;
	private NotificationEngine notificationEngine;

	@Override
	public void setWebapp(TavernaServer webapp) {
		this.webapp = webapp;
	}

	@Required
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	@Required
	public void setListenerFactory(ListenerFactory listenerFactory) {
		this.listenerFactory = listenerFactory;
	}

	@Required
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	@Override
	public PolicyDescription getDescription(UriInfo ui) {
		return new PolicyDescription(ui);
	}

	@Override
	public int getMaxSimultaneousRuns() {
		Integer limit = policy.getMaxRuns(webapp.getPrincipal());
		if (limit == null)
			return policy.getMaxRuns();
		return min(limit.intValue(), policy.getMaxRuns());
	}

	@Override
	public PermittedListeners getPermittedListeners() {
		return new PermittedListeners(
				listenerFactory.getSupportedListenerTypes());
	}

	@Override
	public PermittedWorkflows getPermittedWorkflows() {
		return new PermittedWorkflows(policy.listPermittedWorkflows(webapp
				.getPrincipal()));
	}

	@Override
	public EnabledNotificationFabrics getEnabledNotifiers() {
		return new EnabledNotificationFabrics(
				notificationEngine.listAvailableDispatchers());
	}
}
