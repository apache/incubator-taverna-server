/*
 */
package org.taverna.server.master.interfaces;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.net.URI;
import java.util.List;

import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoDestroyException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Simple policy interface.
 * 
 * @author Donal Fellows
 */
public interface Policy {
	/**
	 * @return The maximum number of runs that the system can support.
	 */
	int getMaxRuns();

	/**
	 * Get the limit on the number of runs for this user.
	 * 
	 * @param user
	 *            Who to get the limit for
	 * @return The maximum number of runs for this user, or <tt>null</tt> if no
	 *         per-user limit is imposed and only system-wide limits are to be
	 *         enforced.
	 */
	Integer getMaxRuns(UsernamePrincipal user);

	/**
	 * Test whether the user can create an instance of the given workflow.
	 * 
	 * @param user
	 *            Who wants to do the creation.
	 * @param workflow
	 *            The workflow they wish to instantiate.
	 * @throws NoCreateException
	 *             If they may not instantiate it.
	 */
	void permitCreate(UsernamePrincipal user, Workflow workflow)
			throws NoCreateException;

	/**
	 * Test whether the user can destroy a workflow instance run or manipulate
	 * its expiry date.
	 * 
	 * @param user
	 *            Who wants to do the deletion.
	 * @param run
	 *            What they want to delete.
	 * @throws NoDestroyException
	 *             If they may not destroy it.
	 */
	void permitDestroy(UsernamePrincipal user, TavernaRun run)
			throws NoDestroyException;

	/**
	 * Return whether the user has access to a particular workflow run.
	 * <b>Note</b> that this does not throw any exceptions!
	 * 
	 * @param user
	 *            Who wants to read the workflow's state.
	 * @param run
	 *            What do they want to read from.
	 * @return Whether they can read it. Note that this check is always applied
	 *         before testing whether the workflow can be updated or deleted by
	 *         the user.
	 */
	boolean permitAccess(UsernamePrincipal user, TavernaRun run);

	/**
	 * Test whether the user can modify a workflow run (other than for its
	 * expiry date).
	 * 
	 * @param user
	 *            Who wants to do the modification.
	 * @param run
	 *            What they want to modify.
	 * @throws NoUpdateException
	 *             If they may not modify it.
	 */
	void permitUpdate(UsernamePrincipal user, TavernaRun run)
			throws NoUpdateException;

	/**
	 * Get the URIs of the workflows that the given user may execute.
	 * 
	 * @param user
	 *            Who are we finding out on behalf of.
	 * @return A list of workflow URIs that they may instantiate, or
	 *         <tt>null</tt> if any workflow may be submitted.
	 */
	List<URI> listPermittedWorkflowURIs(UsernamePrincipal user);

	/**
	 * @return The maximum number of {@linkplain Status#Operating operating}
	 *         runs that the system can support.
	 */
	int getOperatingLimit();

	/**
	 * Set the URIs of the workflows that the given user may execute.
	 * 
	 * @param user
	 *            Who are we finding out on behalf of.
	 * @param permitted
	 *            A list of workflow URIs that they may instantiate.
	 */
	void setPermittedWorkflowURIs(UsernamePrincipal user, List<URI> permitted);
}
