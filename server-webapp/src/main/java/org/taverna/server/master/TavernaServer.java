/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import java.security.Principal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.soap.TavernaServerSOAP;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.InvocationCounter;

/**
 * The methods of the webapp that are accessed by beans other than itself or
 * those which are told directly about it. This exists so that an AOP proxy can
 * be installed around it.
 * 
 * @author Donal Fellows
 */
public interface TavernaServer extends TavernaServerSOAP, TavernaServerREST,
		UriBuilderFactory, ServletContextAware {
	/**
	 * @return Count of the number of external calls into this webapp.
	 */
	@ManagedAttribute(description = "Count of the number of external calls into this webapp.")
	int getInvocationCount();

	/**
	 * @return Current number of runs.
	 */
	@ManagedAttribute(description = "Current number of runs.")
	int getCurrentRunCount();

	/**
	 * @return Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	boolean getLogIncomingWorkflows();

	/**
	 * @param logIncomingWorkflows
	 *            Whether to write submitted workflows to the log.
	 */
	@ManagedAttribute(description = "Whether to write submitted workflows to the log.")
	void setLogIncomingWorkflows(boolean logIncomingWorkflows);

	/**
	 * @return Whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	boolean getLogOutgoingExceptions();

	/**
	 * @param logOutgoing
	 *            Whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	@ManagedAttribute(description = "Whether outgoing exceptions should be logged before being converted to responses.")
	void setLogOutgoingExceptions(boolean logOutgoing);

	/**
	 * @return Whether to permit any new workflow runs to be created.
	 */
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	boolean getAllowNewWorkflowRuns();

	/**
	 * @param allowNewWorkflowRuns
	 *            Whether to permit any new workflow runs to be created.
	 */
	@ManagedAttribute(description = "Whether to permit any new workflow runs to be created; has no effect on existing runs.")
	void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns);

	/**
	 * @param policy
	 *            The policy being installed by Spring.
	 */
	@Required
	void setPolicy(Policy policy);

	/**
	 * @param listenerFactory
	 *            The listener factory being installed by Spring.
	 */
	@Required
	void setListenerFactory(ListenerFactory listenerFactory);

	/**
	 * @param runFactory
	 *            The run factory being installed by Spring.
	 */
	@Required
	void setRunFactory(RunFactory runFactory);

	/**
	 * @param runStore
	 *            The run store being installed by Spring.
	 */
	@Required
	void setRunStore(RunStore runStore);

	/**
	 * @param stateModel
	 *            The state model engine being installed by Spring.
	 */
	@Required
	void setStateModel(ManagementModel stateModel);

	/**
	 * @param mapper
	 *            The identity mapper being installed by Spring.
	 */
	@Required
	void setIdMapper(LocalIdentityMapper mapper);

	/**
	 * @param converter
	 *            The filename converter being installed by Spring.
	 */
	@Required
	void setFileUtils(FilenameUtils converter);

	/**
	 * @param cdBuilder
	 *            The contents descriptor builder being installed by Spring.
	 */
	@Required
	void setContentsDescriptorBuilder(ContentsDescriptorBuilder cdBuilder);

	/**
	 * @param logthem
	 *            Whether to log failures relating to principals.
	 */
	void setLogGetPrincipalFailures(boolean logthem);

	/**
	 * Test whether the current user can do updates to the given run.
	 * 
	 * @param run
	 *            The workflow run to do the test on.
	 */
	void permitUpdate(TavernaRun run) throws NoUpdateException;

	/**
	 * @return The principal of the user currently accessing the webapp.
	 */
	Principal getPrincipal();

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
	TavernaRun getRun(String name) throws UnknownRunException;

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
	Listener makeListener(TavernaRun run, String type, String configuration)
			throws NoListenerException, NoUpdateException;

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
	Permission getPermission(TavernaSecurityContext context, String userName);

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
	void setPermission(TavernaSecurityContext context, String userName,
			Permission permission);

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
	void unregisterRun(String runName, TavernaRun run)
			throws NoDestroyException, UnknownRunException;

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
	Date updateExpiry(TavernaRun run, Date date) throws NoDestroyException;

	/**
	 * @param counter
	 *            The object whose job it is to manage the counting of
	 *            invocations.
	 */
	@Required
	void setInvocationCounter(InvocationCounter counter);
}