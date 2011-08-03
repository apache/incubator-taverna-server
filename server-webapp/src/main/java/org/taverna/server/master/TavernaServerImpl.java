/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static java.lang.Math.min;
import static java.util.Collections.sort;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.ws.handler.MessageContext.PATH_INFO;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.DirEntryReference.newInstance;
import static org.taverna.server.master.common.Namespaces.SERVER_SOAP;
import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.common.Status.Initialized;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.jws.WebService;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.ws.WebServiceContext;

import org.apache.commons.logging.Log;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerImpl.SupportAware;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCredentialException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.NotOwnerException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.notification.NotificationEngine;
import org.taverna.server.master.notification.atom.EventDAO;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.rest.TavernaServerREST.EnabledNotificationFabrics;
import org.taverna.server.master.rest.TavernaServerREST.PermittedListeners;
import org.taverna.server.master.rest.TavernaServerREST.PermittedWorkflows;
import org.taverna.server.master.rest.TavernaServerREST.PolicyView;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.soap.FileContents;
import org.taverna.server.master.soap.PermissionList;
import org.taverna.server.master.soap.TavernaServerSOAP;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;
import org.taverna.server.port_description.OutputDescription;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The core implementation of the web application.
 * 
 * @author Donal Fellows
 */
@Path("/")
@DeclareRoles({ USER, ADMIN })
@WebService(endpointInterface = "org.taverna.server.master.soap.TavernaServerSOAP", serviceName = "TavernaServer", targetNamespace = SERVER_SOAP)
@WSDLDocumentation("An instance of Taverna 2.3 Server Release 1.")
public abstract class TavernaServerImpl implements TavernaServerSOAP,
		TavernaServerREST, TavernaServer {
	/**
	 * The root of descriptions of the server in JMX.
	 */
	public static final String JMX_ROOT = "Taverna:group=Server-v2,name=";

	/** The logger for the server framework. */
	public static final Log log = getLog("Taverna.Server.Webapp");

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// CONNECTIONS TO JMX, SPRING AND CXF

	@Resource
	WebServiceContext jaxws;
	@Context
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private HttpHeaders jaxrsHeaders;

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// STATE VARIABLES AND SPRING SETTERS

	/**
	 * For building descriptions of the expected inputs and actual outputs of a
	 * workflow.
	 */
	private ContentsDescriptorBuilder cdBuilder;
	/**
	 * Utilities for accessing files on the local-worker.
	 */
	private FilenameUtils fileUtils;
	/** How notifications are dispatched. */
	private NotificationEngine notificationEngine;
	/** Main support class. */
	private TavernaServerSupport support;
	/** A storage facility for workflow runs. */
	private RunStore runStore;
	/** Encapsulates the policies applied by this server. */
	private Policy policy;
	/** Where Atom events come from. */
	EventDAO eventSource;

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
	public void setNotificationEngine(NotificationEngine notificationEngine) {
		this.notificationEngine = notificationEngine;
	}

	/**
	 * @param support
	 *            the support to set
	 */
	@Override
	@Required
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	@Required
	public void setRunStore(RunStore runStore) {
		this.runStore = runStore;
	}

	@Override
	@Required
	public void setPolicy(Policy policy) {
		this.policy = policy;
	}

	@Override
	@Required
	public void setEventSource(EventDAO eventSource) {
		this.eventSource = eventSource;
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// REST INTERFACE

	@Override
	@CallCounted
	public ServerDescription describeService(UriInfo ui) {
		return new ServerDescription(ui);
	}

	@Override
	@CallCounted
	public RunList listUsersRuns(UriInfo ui) {
		return new RunList(runs(), ui.getAbsolutePathBuilder().path("{name}"));
	}

	@Override
	@CallCounted
	public Response submitWorkflow(Workflow workflow, UriInfo ui)
			throws NoUpdateException {
		String name = support.buildWorkflow(workflow);
		return created(ui.getAbsolutePathBuilder().path("{uuid}").build(name))
				.build();
	}

	@Override
	@CallCounted
	public int getMaxSimultaneousRuns() {
		return support.getMaxSimultaneousRuns();
	}

	@Override
	@CallCounted
	public TavernaServerRunREST getRunResource(final String runName)
			throws UnknownRunException {
		RunREST rr = makeRunInterface();
		rr.setRun(support.getRun(runName));
		rr.setRunName(runName);
		return rr;
	}

	@Override
	@CallCounted
	public abstract PolicyView getPolicyDescription();

	/**
	 * Construct a RESTful interface to a run.
	 * 
	 * @return The handle to the interface, as decorated by Spring.
	 */
	protected abstract RunREST makeRunInterface();

	/**
	 * Indicates that this is a class that wants to be told by Spring about the
	 * main support bean.
	 * 
	 * @author Donal Fellows
	 */
	interface SupportAware {
		/**
		 * How to tell the bean about the support bean.
		 * 
		 * @param support
		 *            Reference to the support bean.
		 */
		@Required
		void setSupport(TavernaServerSupport support);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE

	@Override
	@CallCounted
	public RunReference[] listRuns() {
		ArrayList<RunReference> ws = new ArrayList<RunReference>();
		UriBuilder ub = getRunUriBuilder();
		for (String runName : runs().keySet())
			ws.add(new RunReference(runName, ub));
		return ws.toArray(new RunReference[ws.size()]);
	}

	@Override
	@CallCounted
	public RunReference submitWorkflow(Workflow workflow)
			throws NoUpdateException {
		String name = support.buildWorkflow(workflow);
		return new RunReference(name, getRunUriBuilder());
	}

	@Override
	@CallCounted
	public Workflow[] getAllowedWorkflows() {
		List<Workflow> workflows = support.getPermittedWorkflows();
		return workflows.toArray(new Workflow[workflows.size()]);
	}

	@Override
	@CallCounted
	public String[] getAllowedListeners() {
		List<String> types = support.getListenerTypes();
		return types.toArray(new String[types.size()]);
	}

	@Override
	@CallCounted
	public String[] getEnabledNotifiers() {
		List<String> dispatchers = notificationEngine
				.listAvailableDispatchers();
		return dispatchers.toArray(new String[dispatchers.size()]);
	}

	@Override
	@CallCounted
	public void destroyRun(String runName) throws UnknownRunException,
			NoUpdateException {
		support.unregisterRun(runName, null);
	}

	@Override
	@CallCounted
	public Workflow getRunWorkflow(String runName) throws UnknownRunException {
		return support.getRun(runName).getWorkflow();
	}

	@Override
	@CallCounted
	public Date getRunExpiry(String runName) throws UnknownRunException {
		return support.getRun(runName).getExpiry();
	}

	@Override
	@CallCounted
	public void setRunExpiry(String runName, Date d)
			throws UnknownRunException, NoUpdateException {
		support.updateExpiry(support.getRun(runName), d);
	}

	@Override
	@CallCounted
	public Date getRunCreationTime(String runName) throws UnknownRunException {
		return support.getRun(runName).getCreationTimestamp();
	}

	@Override
	@CallCounted
	public Date getRunFinishTime(String runName) throws UnknownRunException {
		return support.getRun(runName).getFinishTimestamp();
	}

	@Override
	@CallCounted
	public Date getRunStartTime(String runName) throws UnknownRunException {
		return support.getRun(runName).getStartTimestamp();
	}

	@Override
	@CallCounted
	public Status getRunStatus(String runName) throws UnknownRunException {
		return support.getRun(runName).getStatus();
	}

	@Override
	@CallCounted
	public void setRunStatus(String runName, Status s)
			throws UnknownRunException, NoUpdateException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		w.setStatus(s);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Security

	@Override
	@CallCounted
	public String getRunOwner(String runName) throws UnknownRunException {
		return support.getRun(runName).getSecurityContext().getOwner()
				.getName();
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
		TavernaRun run = support.getRun(runName);
		TavernaSecurityContext c = run.getSecurityContext();
		if (!c.getOwner().equals(support.getPrincipal()))
			throw new NotOwnerException();
		if (initialOnly && run.getStatus() != Initialized)
			throw new BadStateChangeException();
		return c;
	}

	@Override
	@CallCounted
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
	@CallCounted
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
	@CallCounted
	public void deleteRunCredential(String runName, String credentialID)
			throws UnknownRunException, NotOwnerException,
			NoCredentialException, BadStateChangeException {
		getRunSecurityContext(runName, true).deleteCredential(
				new Credential.Dummy(credentialID));
	}

	@Override
	@CallCounted
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
	@CallCounted
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
	@CallCounted
	public void deleteRunCertificates(String runName, String certificateID)
			throws UnknownRunException, NotOwnerException,
			NoCredentialException, BadStateChangeException {
		TavernaSecurityContext c = getRunSecurityContext(runName, true);
		Trust toDelete = new Trust();
		toDelete.id = certificateID;
		c.deleteTrusted(toDelete);
	}

	@Override
	@CallCounted
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
	@CallCounted
	public void setRunPermission(String runName, String userName,
			Permission permission) throws UnknownRunException,
			NotOwnerException {
		try {
			support.setPermission(getRunSecurityContext(runName, false),
					userName, permission);
		} catch (BadStateChangeException e) {
			log.error("unexpected error from internal API", e);
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Filesystem connection

	@Override
	@CallCounted
	public OutputDescription getRunOutputDescription(String runName)
			throws UnknownRunException, BadStateChangeException,
			FilesystemAccessException, NoDirectoryEntryException {
		TavernaRun run = support.getRun(runName);
		if (run.getStatus() == Initialized) {
			throw new BadStateChangeException(
					"may not get output description in initial state");
		}
		return cdBuilder.makeOutputDescriptor(run, null);
	}

	@Override
	@CallCounted
	public DirEntryReference[] getRunDirectoryContents(String runName,
			DirEntryReference d) throws UnknownRunException,
			FilesystemAccessException, NoDirectoryEntryException {
		List<DirEntryReference> result = new ArrayList<DirEntryReference>();
		for (DirectoryEntry e : fileUtils.getDirectory(support.getRun(runName),
				d).getContents())
			result.add(newInstance(null, e));
		return result.toArray(new DirEntryReference[result.size()]);
	}

	@Override
	@CallCounted
	public byte[] getRunDirectoryAsZip(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		return fileUtils.getDirectory(support.getRun(runName), d)
				.getContentsAsZip();
	}

	@Override
	@CallCounted
	public DirEntryReference makeRunDirectory(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		Directory dir = fileUtils.getDirectory(w, parent).makeSubdirectory(
				support.getPrincipal(), name);
		return newInstance(null, dir);
	}

	@Override
	@CallCounted
	public DirEntryReference makeRunFile(String runName,
			DirEntryReference parent, String name) throws UnknownRunException,
			NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		File f = fileUtils.getDirectory(w, parent).makeEmptyFile(
				support.getPrincipal(), name);
		return newInstance(null, f);
	}

	@Override
	@CallCounted
	public void destroyRunDirectoryEntry(String runName, DirEntryReference d)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		fileUtils.getDirEntry(w, d).destroy();
	}

	@Override
	@CallCounted
	public byte[] getRunFileContents(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		File f = fileUtils.getFile(support.getRun(runName), d);
		return f.getContents(0, -1);
	}

	@Override
	@CallCounted
	public void setRunFileContents(String runName, DirEntryReference d,
			byte[] newContents) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		fileUtils.getFile(w, d).setContents(newContents);
	}

	@Override
	@CallCounted
	public FileContents getRunFileContentsMTOM(String runName,
			DirEntryReference d) throws UnknownRunException,
			FilesystemAccessException, NoDirectoryEntryException {
		File f = fileUtils.getFile(support.getRun(runName), d);
		FileContents fc = new FileContents();
		fc.setFile(f, support.getEstimatedContentType(f));
		return fc;
	}

	@Override
	@CallCounted
	public void setRunFileContentsMTOM(String runName, FileContents newContents)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		try {
			newContents.writeToFile(fileUtils.getFile(w, newContents.name));
		} catch (IOException e) {
			throw new FilesystemAccessException(
					"problem reading from data source", e);
		}
	}

	@Override
	@CallCounted
	public String getRunFileType(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		return support.getEstimatedContentType(fileUtils.getFile(
				support.getRun(runName), d));
	}

	@Override
	@CallCounted
	public long getRunFileLength(String runName, DirEntryReference d)
			throws UnknownRunException, FilesystemAccessException,
			NoDirectoryEntryException {
		return fileUtils.getFile(support.getRun(runName), d).getSize();
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SOAP INTERFACE - Run listeners

	@Override
	@CallCounted
	public String[] getRunListeners(String runName) throws UnknownRunException {
		TavernaRun w = support.getRun(runName);
		List<String> result = new ArrayList<String>();
		for (Listener l : w.getListeners())
			result.add(l.getName());
		return result.toArray(new String[result.size()]);
	}

	@Override
	@CallCounted
	public String addRunListener(String runName, String listenerType,
			String configuration) throws UnknownRunException,
			NoUpdateException, NoListenerException {
		return support.makeListener(support.getRun(runName), listenerType,
				configuration).getName();
	}

	@Override
	@CallCounted
	public String getRunListenerConfiguration(String runName,
			String listenerName) throws UnknownRunException,
			NoListenerException {
		return support.getListener(runName, listenerName).getConfiguration();
	}

	@Override
	@CallCounted
	public String[] getRunListenerProperties(String runName, String listenerName)
			throws UnknownRunException, NoListenerException {
		return support.getListener(runName, listenerName).listProperties()
				.clone();
	}

	@Override
	@CallCounted
	public String getRunListenerProperty(String runName, String listenerName,
			String propName) throws UnknownRunException, NoListenerException {
		return support.getListener(runName, listenerName).getProperty(propName);
	}

	@Override
	@CallCounted
	public void setRunListenerProperty(String runName, String listenerName,
			String propName, String value) throws UnknownRunException,
			NoUpdateException, NoListenerException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		Listener l = support.getListener(w, listenerName);
		try {
			l.getProperty(propName); // sanity check!
			l.setProperty(propName, value);
		} catch (RuntimeException e) {
			throw new NoListenerException("problem setting property: "
					+ e.getMessage(), e);
		}
	}

	@Override
	@CallCounted
	public InputDescription getRunInputs(String runName)
			throws UnknownRunException {
		return new InputDescription(support.getRun(runName));
	}

	@Override
	@CallCounted
	public String getRunOutputBaclavaFile(String runName)
			throws UnknownRunException {
		return support.getRun(runName).getOutputBaclavaFile();
	}

	@Override
	@CallCounted
	public void setRunInputBaclavaFile(String runName, String fileName)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		w.setInputBaclavaFile(fileName);
	}

	@Override
	@CallCounted
	public void setRunInputPortFile(String runName, String portName,
			String portFilename) throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		Input i = support.getInput(w, portName);
		if (i == null)
			i = w.makeInput(portName);
		i.setFile(portFilename);
	}

	@Override
	@CallCounted
	public void setRunInputPortValue(String runName, String portName,
			String portValue) throws UnknownRunException, NoUpdateException,
			BadStateChangeException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		Input i = support.getInput(w, portName);
		if (i == null)
			i = w.makeInput(portName);
		i.setValue(portValue);
	}

	@Override
	@CallCounted
	public void setRunOutputBaclavaFile(String runName, String outputFile)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		TavernaRun w = support.getRun(runName);
		support.permitUpdate(w);
		w.setOutputBaclavaFile(outputFile);
	}

	@Override
	@CallCounted
	public org.taverna.server.port_description.InputDescription getRunInputDescriptor(
			String runName) throws UnknownRunException {
		return cdBuilder.makeInputDescriptor(support.getRun(runName), null);
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
	// SUPPORT METHODS

	@Override
	public void initObsoleteSecurity(TavernaSecurityContext c) {
		/*
		 * These next pieces of security initialisation are (hopefully) obsolete
		 * now that we use Spring Security, but we keep them Just In Case.
		 */
		boolean doRESTinit = true;
		if (jaxws != null) {
			try {
				javax.xml.ws.handler.MessageContext msgCtxt = jaxws
						.getMessageContext();
				if (msgCtxt != null) {
					doRESTinit = false;
					c.initializeSecurityFromSOAPContext(msgCtxt);
				}
			} catch (IllegalStateException e) {
				/* ignore; not much we can do */
			}
		}
		if (doRESTinit && jaxrsHeaders != null)
			c.initializeSecurityFromRESTContext(jaxrsHeaders);
	}

	/**
	 * A creator of substitute {@link URI} builders.
	 * 
	 * @return A URI builder configured so that it takes a path parameter that
	 *         corresponds to the run ID (but with no such ID applied).
	 */
	UriBuilder getRunUriBuilder() {
		return getBaseUriBuilder().path("runs/{uuid}");
	}

	@Override
	public UriBuilder getRunUriBuilder(TavernaRun run) {
		return fromUri(getRunUriBuilder().build(run.getId()));
	}

	@Override
	public UriBuilder getBaseUriBuilder() {
		if (jaxws == null || jaxws.getMessageContext() == null)
			// Hack to make the test suite work
			return fromUri("/taverna-server/rest/");
		String pathInfo = (String) jaxws.getMessageContext().get(PATH_INFO);
		pathInfo = pathInfo.replaceFirst("/soap$", "/rest/");
		pathInfo = pathInfo.replaceFirst("/rest/.+$", "/rest/");
		return fromUri(pathInfo);
	}

	private Map<String, TavernaRun> runs() {
		return runStore.listRuns(support.getPrincipal(), policy);
	}
}

/**
 * RESTful interface to the policies of a Taverna Server installation.
 * 
 * @author Donal Fellows
 */
class PolicyREST implements PolicyView, SupportAware {
	private TavernaServerSupport support;
	private Policy policy;
	private ListenerFactory listenerFactory;
	private NotificationEngine notificationEngine;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
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
	@CallCounted
	public int getMaxSimultaneousRuns() {
		Integer limit = policy.getMaxRuns(support.getPrincipal());
		if (limit == null)
			return policy.getMaxRuns();
		return min(limit.intValue(), policy.getMaxRuns());
	}

	@Override
	@CallCounted
	public PermittedListeners getPermittedListeners() {
		return new PermittedListeners(
				listenerFactory.getSupportedListenerTypes());
	}

	@Override
	@CallCounted
	public PermittedWorkflows getPermittedWorkflows() {
		return new PermittedWorkflows(policy.listPermittedWorkflows(support
				.getPrincipal()));
	}

	@Override
	@CallCounted
	public EnabledNotificationFabrics getEnabledNotifiers() {
		return new EnabledNotificationFabrics(
				notificationEngine.listAvailableDispatchers());
	}
}
