/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static eu.medsea.util.MimeUtil.UNKNOWN_MIME_TYPE;
import static eu.medsea.util.MimeUtil.getExtensionMimeTypes;
import static eu.medsea.util.MimeUtil.getMimeType;
import static java.lang.Math.min;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.springframework.jmx.support.MetricType.COUNTER;
import static org.springframework.jmx.support.MetricType.GAUGE;
import static org.taverna.server.master.TavernaServer.JMX_ROOT;
import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.taverna.server.master.api.ManagementModel;
import org.taverna.server.master.api.TavernaServerBean;
import org.taverna.server.master.common.Capability;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.ProfileList;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.common.version.Version;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.identity.WorkflowInternalAuthProvider.WorkflowSelfAuthority;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.handler.T2FlowDocumentHandler;
import org.taverna.server.master.utils.CapabilityLister;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.InvocationCounter;
import org.taverna.server.master.utils.UsernamePrincipal;

import uk.org.taverna.scufl2.api.profiles.Profile;

/**
 * Web application support utilities.
 * 
 * @author Donal Fellows
 */
@ManagedResource(objectName = JMX_ROOT + "Webapp", description = "The main Taverna Server "
		+ Version.JAVA + " web-application interface.")
public class TavernaServerSupport {
	/** The main webapp log. */
	private Log log = getLog("Taverna.Server.Webapp");
	private Log accessLog = getLog("Taverna.Server.Webapp.Access");;
	/** Bean used to log counts of external calls. */
	private InvocationCounter counter;
	/** A storage facility for workflow runs. */
	private RunStore runStore;
	/** Encapsulates the policies applied by this server. */
	private Policy policy;
	/** Connection to the persistent state of this service. */
	private ManagementModel stateModel;
	/** A factory for event listeners to attach to workflow runs. */
	private ListenerFactory listenerFactory;
	/** A factory for workflow runs. */
	private RunFactory runFactory;
	/** How to map the user ID to who to run as. */
	private LocalIdentityMapper idMapper;
	/** The code that is coupled to CXF. */
	private TavernaServerBean webapp;
	/** How to handle files. */
	private FilenameUtils fileUtils;
	/** How to get the server capabilities. */
	private CapabilityLister capabilitySource;
	/**
	 * Whether to log failures during principal retrieval. Should be normally on
	 * as it indicates a serious problem, but can be switched off for testing.
	 */
	private boolean logGetPrincipalFailures = true;
	private Map<String, String> contentTypeMap;
	/** Number of bytes to read when guessing the MIME type. */
	private static final int SAMPLE_SIZE = 1024;
	/** Number of bytes to ask for when copying a stream to a file. */
	private static final int TRANSFER_SIZE = 32768;

	@PreDestroy
	void closeLog() {
		log = null;
	}

	/**
	 * @return Count of the number of external calls into this webapp.
	 */
	@ManagedMetric(description = "Count of the number of external calls into this webapp.", metricType = COUNTER, category = "throughput")
	public int getInvocationCount() {
		return counter.getCount();
	}

	/**
	 * @return Current number of runs.
	 */
	@ManagedMetric(description = "Current number of runs.", metricType = GAUGE, category = "utilization")
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

	/**
	 * @return The server's version identifier.
	 */
	@ManagedAttribute(description = "The installed version of the server.")
	public String getServerVersion() {
		return VersionedElement.VERSION + " " + VersionedElement.REVISION + " "
				+ VersionedElement.TIMESTAMP;
	}

	@ManagedAttribute(description = "The URIs of the workfows that this server will allow to be instantiated.")
	public URI[] getPermittedWorkflowURIs() {
		List<URI> pw = policy.listPermittedWorkflowURIs(null);
		if (pw == null)
			return new URI[0];
		return pw.toArray(new URI[pw.size()]);
	}

	@ManagedAttribute(description = "The URIs of the workfows that this server will allow to be instantiated.")
	public void setPermittedWorkflowURIs(URI[] pw) {
		if (pw == null)
			policy.setPermittedWorkflowURIs(null, new ArrayList<URI>());
		else
			policy.setPermittedWorkflowURIs(null, Arrays.asList(pw));
	}

	public int getMaxSimultaneousRuns() {
		Integer limit = policy.getMaxRuns(getPrincipal());
		if (limit == null)
			return policy.getMaxRuns();
		return min(limit.intValue(), policy.getMaxRuns());
	}

	@Autowired
	private T2FlowDocumentHandler t2flowHandler;

	public Workflow getWorkflowDocumentFromURI(URI uri)
			throws WebApplicationException, IOException {
		URLConnection conn = uri.toURL().openConnection();
		conn.setRequestProperty("Accept", T2FLOW);
		conn.connect();
		// Tricky point: we know the reader part of the handler only cares
		// about the stream argument.
		return t2flowHandler.readFrom(null, null, null, null, null,
				conn.getInputStream());
	}

	public List<String> getListenerTypes() {
		return listenerFactory.getSupportedListenerTypes();
	}

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
	 * @param counter
	 *            The object whose job it is to manage the counting of
	 *            invocations. Installed by Spring.
	 */
	@Required
	public void setInvocationCounter(InvocationCounter counter) {
		this.counter = counter;
	}

	/**
	 * @param webapp
	 *            The web-app being installed by Spring.
	 */
	@Required
	public void setWebapp(TavernaServerBean webapp) {
		this.webapp = webapp;
	}

	/**
	 * @param fileUtils
	 *            The file handling utilities.
	 */
	@Required
	public void setFileUtils(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	/**
	 * @param logthem
	 *            Whether to log failures relating to principals.
	 */
	public void setLogGetPrincipalFailures(boolean logthem) {
		logGetPrincipalFailures = logthem;
	}

	public Map<String, String> getContentTypeMap() {
		return contentTypeMap;
	}

	/**
	 * Mapping from filename suffixes (e.g., "baclava") to content types.
	 * 
	 * @param contentTypeMap
	 *            The mapping to install.
	 */
	@Required
	public void setContentTypeMap(Map<String, String> contentTypeMap) {
		this.contentTypeMap = contentTypeMap;
	}

	@Required
	public void setCapabilitySource(CapabilityLister capabilitySource) {
		this.capabilitySource = capabilitySource;
	}

	/**
	 * Test whether the current user can do updates to the given run.
	 * 
	 * @param run
	 *            The workflow run to do the test on.
	 * @throws NoUpdateException
	 *             If the current user is not permitted to update the run.
	 */
	public void permitUpdate(@Nonnull TavernaRun run) throws NoUpdateException {
		if (isSuperUser()) {
			accessLog
					.warn("check for admin powers passed; elevated access rights granted for update");
			return; // Superusers are fully authorized to access others things
		}
		if (getSelfAuthority() != null) {
			// At this point, must already be accessing self as that is checked
			// in getRun().
			return;
		}
		policy.permitUpdate(getPrincipal(), run);
	}

	/**
	 * Test whether the current user can destroy or control the lifespan of the
	 * given run.
	 * 
	 * @param run
	 *            The workflow run to do the test on.
	 * @throws NoDestroyException
	 *             If the current user is not permitted to destroy the run.
	 */
	public void permitDestroy(TavernaRun run) throws NoDestroyException {
		if (isSuperUser()) {
			accessLog
					.warn("check for admin powers passed; elevated access rights granted for destroy");
			return; // Superusers are fully authorized to access others things
		}
		if (getSelfAuthority() != null)
			throw new NoDestroyException();
		policy.permitDestroy(getPrincipal(), run);
	}

	/**
	 * Gets the identity of the user currently accessing the webapp, which is
	 * stored in a thread-safe way in the webapp's container's context.
	 * 
	 * @return The identity of the user accessing the webapp.
	 */
	@Nonnull
	public UsernamePrincipal getPrincipal() {
		try {
			Authentication auth = SecurityContextHolder.getContext()
					.getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				if (logGetPrincipalFailures)
					log.warn("failed to get auth; going with <NOBODY>");
				return new UsernamePrincipal("<NOBODY>");
			}
			return new UsernamePrincipal(auth);
		} catch (RuntimeException e) {
			if (logGetPrincipalFailures)
				log.info("failed to map principal", e);
			throw e;
		}
	}

	private WorkflowSelfAuthority getSelfAuthority() {
		try {
			Authentication a = SecurityContextHolder.getContext()
					.getAuthentication();
			for (GrantedAuthority ga : a.getAuthorities())
				if (ga instanceof WorkflowSelfAuthority)
					return (WorkflowSelfAuthority) ga;
		} catch (RuntimeException e) {
		}
		return null;
	}

	/**
	 * Obtain the workflow run with a particular name.
	 * 
	 * @param name
	 *            The name of the run to look up.
	 * @return A workflow run handle that the current user has at least
	 *         permission to read.
	 * @throws UnknownRunException
	 *             If the workflow run doesn't exist or the current user doesn't
	 *             have permission to see it.
	 */
	@Nonnull
	public TavernaRun getRun(@Nonnull String name) throws UnknownRunException {
		if (isSuperUser()) {
			accessLog
					.info("check for admin powers passed; elevated access rights granted for read");
			return runStore.getRun(name);
		}
		WorkflowSelfAuthority wsa = getSelfAuthority();
		if (wsa != null) {
			if (wsa.getWorkflowID().equals(name))
				return runStore.getRun(name);
			throw new UnknownRunException();
		}
		return runStore.getRun(getPrincipal(), policy, name);
	}

	/**
	 * Construct a listener attached to the given run.
	 * 
	 * @param run
	 *            The workflow run to attach the listener to.
	 * @param type
	 *            The name of the type of run to create.
	 * @param configuration
	 *            The configuration description to pass into the listener. The
	 *            format of this string is up to the listener to define.
	 * @return A handle to the listener which can be used to further configure
	 *         any properties.
	 * @throws NoListenerException
	 *             If the listener type is unrecognized or the configuration is
	 *             invalid.
	 * @throws NoUpdateException
	 *             If the run does not permit the current user to add listeners
	 *             (or perform other types of update).
	 */
	@Nonnull
	public Listener makeListener(@Nonnull TavernaRun run, @Nonnull String type,
			@Nonnull String configuration) throws NoListenerException,
			NoUpdateException {
		permitUpdate(run);
		return listenerFactory.makeListener(run, type, configuration);
	}

	/**
	 * Obtain a listener that is already attached to a workflow run.
	 * 
	 * @param run
	 *            The workflow run to search.
	 * @param listenerName
	 *            The name of the listener to look up.
	 * @return The listener instance interface.
	 * @throws NoListenerException
	 *             If no listener with that name exists.
	 */
	@Nonnull
	public Listener getListener(TavernaRun run, String listenerName)
			throws NoListenerException {
		for (Listener l : run.getListeners())
			if (l.getName().equals(listenerName))
				return l;
		throw new NoListenerException();
	}

	/**
	 * Obtain a property from a listener that is already attached to a workflow
	 * run.
	 * 
	 * @param runName
	 *            The ID of the workflow run to search.
	 * @param listenerName
	 *            The name of the listener to look up in.
	 * @param propertyName
	 *            The name of the property to fetch.
	 * @return The property value.
	 * @throws NoListenerException
	 *             If no listener with that name exists, or no property with
	 *             that name exists.
	 * @throws UnknownRunException
	 *             If no run with that name exists.
	 */
	@Nonnull
	public String getProperty(String runName, String listenerName,
			String propertyName) throws NoListenerException,
			UnknownRunException {
		return getListener(runName, listenerName).getProperty(propertyName);
	}

	/**
	 * Obtain a property from a listener that is already attached to a workflow
	 * run.
	 * 
	 * @param run
	 *            The workflow run to search.
	 * @param listenerName
	 *            The name of the listener to look up in.
	 * @param propertyName
	 *            The name of the property to fetch.
	 * @return The property value.
	 * @throws NoListenerException
	 *             If no listener with that name exists, or no property with
	 *             that name exists.
	 */
	@Nonnull
	public String getProperty(TavernaRun run, String listenerName,
			String propertyName) throws NoListenerException {
		return getListener(run, listenerName).getProperty(propertyName);
	}

	/**
	 * Get the permission description for the given user.
	 * 
	 * @param context
	 *            A security context associated with a particular workflow run.
	 *            Note that only the owner of a workflow run may get the
	 *            security context in the first place.
	 * @param userName
	 *            The name of the user to look up the permission for.
	 * @return A permission description.
	 */
	@Nonnull
	public Permission getPermission(@Nonnull TavernaSecurityContext context,
			@Nonnull String userName) {
		if (context.getPermittedDestroyers().contains(userName))
			return Permission.Destroy;
		if (context.getPermittedUpdaters().contains(userName))
			return Permission.Update;
		if (context.getPermittedReaders().contains(userName))
			return Permission.Read;
		return Permission.None;
	}

	/**
	 * Set the permissions for the given user.
	 * 
	 * @param context
	 *            A security context associated with a particular workflow run.
	 *            Note that only the owner of a workflow run may get the
	 *            security context in the first place.
	 * @param userName
	 *            The name of the user to set the permission for.
	 * @param permission
	 *            The description of the permission to grant. Note that the
	 *            owner of a workflow run always has the equivalent of
	 *            {@link Permission#Destroy}; this is always enforced before
	 *            checking for other permissions.
	 */
	public void setPermission(TavernaSecurityContext context, String userName,
			Permission permission) {
		Set<String> permSet;
		boolean doRead = false, doWrite = false, doKill = false;

		switch (permission) {
		case Destroy:
			doKill = true;
		case Update:
			doWrite = true;
		case Read:
			doRead = true;
		default:
			break;
		}

		permSet = context.getPermittedReaders();
		if (doRead) {
			if (!permSet.contains(userName)) {
				permSet = new HashSet<>(permSet);
				permSet.add(userName);
				context.setPermittedReaders(permSet);
			}
		} else if (permSet.contains(userName)) {
			permSet = new HashSet<>(permSet);
			permSet.remove(userName);
			context.setPermittedReaders(permSet);
		}

		permSet = context.getPermittedUpdaters();
		if (doWrite) {
			if (!permSet.contains(userName)) {
				permSet = new HashSet<>(permSet);
				permSet.add(userName);
				context.setPermittedUpdaters(permSet);
			}
		} else if (permSet.contains(userName)) {
			permSet = new HashSet<>(permSet);
			permSet.remove(userName);
			context.setPermittedUpdaters(permSet);
		}

		permSet = context.getPermittedDestroyers();
		if (doKill) {
			if (!permSet.contains(userName)) {
				permSet = new HashSet<>(permSet);
				permSet.add(userName);
				context.setPermittedDestroyers(permSet);
			}
		} else if (permSet.contains(userName)) {
			permSet = new HashSet<>(permSet);
			permSet.remove(userName);
			context.setPermittedDestroyers(permSet);
		}
	}

	public Map<String, Permission> getPermissionMap(
			TavernaSecurityContext context) {
		Map<String, Permission> perm = new HashMap<>();
		for (String u : context.getPermittedReaders())
			perm.put(u, Permission.Read);
		for (String u : context.getPermittedUpdaters())
			perm.put(u, Permission.Update);
		for (String u : context.getPermittedDestroyers())
			perm.put(u, Permission.Destroy);
		return perm;
	}

	/**
	 * Stops a run from being possible to be looked up and destroys it.
	 * 
	 * @param runName
	 *            The name of the run.
	 * @param run
	 *            The workflow run. <i>Must</i> correspond to the name.
	 * @throws NoDestroyException
	 *             If the user is not permitted to destroy the workflow run.
	 * @throws UnknownRunException
	 *             If the run is unknown (e.g., because it is already
	 *             destroyed).
	 */
	public void unregisterRun(@Nonnull String runName, @Nonnull TavernaRun run)
			throws NoDestroyException, UnknownRunException {
		if (run == null)
			run = getRun(runName);
		permitDestroy(run);
		runStore.unregisterRun(runName);
		run.destroy();
	}

	/**
	 * Changes the expiry date of a workflow run. The expiry date is when the
	 * workflow run becomes eligible for automated destruction.
	 * 
	 * @param run
	 *            The handle to the workflow run.
	 * @param date
	 *            When the workflow run should be expired.
	 * @return When the workflow run will actually be expired.
	 * @throws NoDestroyException
	 *             If the user is not permitted to destroy the workflow run.
	 *             (Note that lifespan management requires the ability to
	 *             destroy.)
	 */
	@Nonnull
	public Date updateExpiry(@Nonnull TavernaRun run, @Nonnull Date date)
			throws NoDestroyException {
		permitDestroy(run);
		run.setExpiry(date);
		return run.getExpiry();
	}

	/**
	 * Manufacture a workflow run instance.
	 * 
	 * @param workflow
	 *            The workflow document (t2flow, scufl2?) to instantiate.
	 * @return The ID of the created workflow run.
	 * @throws NoCreateException
	 *             If the user is not permitted to create workflows.
	 */
	public String buildWorkflow(Workflow workflow) throws NoCreateException {
		UsernamePrincipal p = getPrincipal();
		if (getSelfAuthority() != null)
			throw new NoCreateException(
					"runs may not create workflows on their host server");
		if (!stateModel.getAllowNewWorkflowRuns())
			throw new NoCreateException("run creation not currently enabled");
		try {
			if (stateModel.getLogIncomingWorkflows()) {
				log.info(workflow.marshal());
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
			c.initializeSecurityFromContext(SecurityContextHolder.getContext());
			/*
			 * These next pieces of security initialisation are (hopefully)
			 * obsolete now that we use Spring Security, but we keep them Just
			 * In Case.
			 */
			boolean doRESTinit = webapp.initObsoleteSOAPSecurity(c);
			if (doRESTinit)
				webapp.initObsoleteRESTSecurity(c);
		} catch (Exception e) {
			log.error("failed to build workflow run worker", e);
			throw new NoCreateException("failed to build workflow run worker");
		}

		return runStore.registerRun(run);
	}

	private boolean isSuperUser() {
		try {
			Authentication auth = SecurityContextHolder.getContext()
					.getAuthentication();
			if (auth == null || !auth.isAuthenticated())
				return false;
			UserDetails details = (UserDetails) auth.getPrincipal();
			if (log.isDebugEnabled())
				log.debug("checking for admin role for user <" + auth.getName()
						+ "> in collection " + details.getAuthorities());
			return details.getAuthorities().contains(ADMIN);
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * Get a particular input to a workflow run.
	 * 
	 * @param run
	 *            The workflow run to search.
	 * @param portName
	 *            The name of the input.
	 * @return The handle of the input, or <tt>null</tt> if no such handle
	 *         exists.
	 */
	@Nullable
	public Input getInput(TavernaRun run, String portName) {
		for (Input i : run.getInputs())
			if (i.getName().equals(portName))
				return i;
		return null;
	}

	/**
	 * Get a listener attached to a run.
	 * 
	 * @param runName
	 *            The name of the run to look up
	 * @param listenerName
	 *            The name of the listener.
	 * @return The handle of the listener.
	 * @throws NoListenerException
	 *             If no such listener exists.
	 * @throws UnknownRunException
	 *             If no such workflow run exists, or if the user does not have
	 *             permission to access it.
	 */
	public Listener getListener(String runName, String listenerName)
			throws NoListenerException, UnknownRunException {
		return getListener(getRun(runName), listenerName);
	}

	/**
	 * Given a file, produce a guess at its content type. This uses the content
	 * type map property, and if that search fails it falls back on the Medsea
	 * mime type library.
	 * 
	 * @param f
	 *            The file handle.
	 * @return The content type. If all else fails, produces good old
	 *         "application/octet-stream".
	 */
	@Nonnull
	public String getEstimatedContentType(@Nonnull File f) {
		String name = f.getName();
		for (int idx = name.indexOf('.'); idx != -1; idx = name.indexOf('.',
				idx + 1)) {
			String mt = contentTypeMap.get(name.substring(idx + 1));
			if (mt != null)
				return mt;
		}
		@Nonnull
		String type = getExtensionMimeTypes(name);
		if (!type.equals(UNKNOWN_MIME_TYPE))
			return type;
		try {
			return getMimeType(new ByteArrayInputStream(f.getContents(0,
					SAMPLE_SIZE)));
		} catch (FilesystemAccessException e) {
			return type;
		}
	}

	public void copyDataToFile(DataHandler handler, File file)
			throws FilesystemAccessException {
		try {
			copyStreamToFile(handler.getInputStream(), file);
		} catch (IOException e) {
			throw new FilesystemAccessException(
					"problem constructing stream from data source", e);
		}
	}

	public void copyDataToFile(URI uri, File file)
			throws MalformedURLException, FilesystemAccessException,
			IOException {
		copyStreamToFile(uri.toURL().openStream(), file);
	}

	public void copyStreamToFile(InputStream stream, File file)
			throws FilesystemAccessException {
		String name = file.getFullName();
		long total = 0;
		try {
			byte[] buffer = new byte[TRANSFER_SIZE];
			boolean first = true;
			while (true) {
				int len = stream.read(buffer);
				if (len < 0)
					break;
				total += len;
				if (log.isDebugEnabled())
					log.debug("read " + len
							+ " bytes from source stream (total: " + total
							+ ") bound for " + name);
				if (len == buffer.length) {
					if (first)
						file.setContents(buffer);
					else
						file.appendContents(buffer);
				} else {
					byte[] newBuf = new byte[len];
					System.arraycopy(buffer, 0, newBuf, 0, len);
					if (first)
						file.setContents(newBuf);
					else
						file.appendContents(newBuf);
				}
				first = false;
			}
		} catch (IOException exn) {
			throw new FilesystemAccessException("failed to transfer bytes", exn);
		}
	}

	/**
	 * Build a description of the profiles supported by a workflow. Note that we
	 * expect the set of profiles to be fairly small.
	 * 
	 * @param workflow
	 *            The workflow to describe the profiles of.
	 * @return The descriptor (which might be empty).
	 */
	public ProfileList getProfileDescriptor(Workflow workflow) {
		ProfileList result = new ProfileList();
		String main = workflow.getMainProfileName();
		for (Profile p : workflow.getProfiles()) {
			ProfileList.Info i = new ProfileList.Info();
			i.name = p.getName();
			if (main != null && main.equals(i.name))
				i.main = true;
			result.profile.add(i);
		}
		return result;
	}

	public boolean getAllowStartWorkflowRuns() {
		return runFactory.isAllowingRunsToStart();
	}

	/**
	 * The list of filenames that logs may occupy.
	 */
	private static final String[] LOGS = { "logs/detail.log.4",
			"logs/detail.log.3", "logs/detail.log.2", "logs/detail.log.1",
			"logs/detail.log" };

	public FileConcatenation getLogs(TavernaRun run) {
		FileConcatenation fc = new FileConcatenation();
		for (String name : LOGS) {
			try {
				fc.add(fileUtils.getFile(run, name));
			} catch (FilesystemAccessException | NoDirectoryEntryException e) {
				// Ignore
			}
		}
		return fc;
	}

	@Nonnull
	public List<Capability> getCapabilities() {
		return capabilitySource.getCapabilities();
	}

	static final String PROV_BUNDLE = "out.bundle.zip";

	public FileConcatenation getProv(TavernaRun run) {
		FileConcatenation fc = new FileConcatenation();
		try {
			fc.add(fileUtils.getFile(run, PROV_BUNDLE));
		} catch (FilesystemAccessException | NoDirectoryEntryException e) {
			// Ignore
		}
		return fc;
	}
}
