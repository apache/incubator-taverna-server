package org.apache.taverna.server.master.mocks;
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

import org.apache.taverna.server.master.common.Workflow;
import org.apache.taverna.server.master.exceptions.NoCreateException;
import org.apache.taverna.server.master.exceptions.NoDestroyException;
import org.apache.taverna.server.master.exceptions.NoUpdateException;
import org.apache.taverna.server.master.interfaces.Policy;
import org.apache.taverna.server.master.interfaces.TavernaRun;
import org.apache.taverna.server.master.utils.UsernamePrincipal;

/**
 * A very simple (and unsafe) security model. The number of runs is configurable
 * through Spring (or 10 if unconfigured) with no per-user limits supported, all
 * workflows are permitted, and all identified users may create a workflow run.
 * Any user may read off information about any run, but only its owner may
 * modify or destroy it.
 * <p>
 * Note that this is a <i>Policy Enforcement Point</i> for access control to
 * individual workflows.
 * 
 * @author Donal Fellows
 */
public class SimpleServerPolicy implements Policy {
	private int maxRuns = 10;
	private int cleanerInterval;
	SimpleNonpersistentRunStore store;

	public void setMaxRuns(int maxRuns) {
		this.maxRuns = maxRuns;
	}

	@Override
	public int getMaxRuns() {
		return maxRuns;
	}

	@Override
	public Integer getMaxRuns(UsernamePrincipal p) {
		return null; // No per-user limits
	}

	public int getCleanerInterval() {
		return cleanerInterval;
	}

	/**
	 * Sets how often the store of workflow runs will try to clean out expired
	 * runs.
	 * 
	 * @param intervalInSeconds
	 */
	public void setCleanerInterval(int intervalInSeconds) {
		cleanerInterval = intervalInSeconds;
		if (store != null)
			store.cleanerIntervalUpdated(intervalInSeconds);
	}

	@Override
	public boolean permitAccess(UsernamePrincipal p, TavernaRun run) {
		// No secrets here!
		return true;
	}

	@Override
	public void permitCreate(UsernamePrincipal p, Workflow workflow)
			throws NoCreateException {
		// Only identified users may create
		if (p == null)
			throw new NoCreateException();
		// Global run count limit enforcement
		if (store.listRuns(p, this).size() >= maxRuns)
			throw new NoCreateException();
		// Per-user run count enforcement would come here
	}

	@Override
	public void permitDestroy(UsernamePrincipal p, TavernaRun run)
			throws NoDestroyException {
		// Only the creator may destroy
		if (p == null || !p.equals(run.getSecurityContext().getOwner()))
			throw new NoDestroyException();
	}

	@Override
	public void permitUpdate(UsernamePrincipal p, TavernaRun run)
			throws NoUpdateException {
		// Only the creator may change
		if (p == null || !p.equals(run.getSecurityContext().getOwner()))
			throw new NoUpdateException();
	}

	@Override
	public int getOperatingLimit() {
		return 1;
	}

	@Override
	public List<URI> listPermittedWorkflowURIs(UsernamePrincipal user) {
		return null;
	}

	@Override
	public void setPermittedWorkflowURIs(UsernamePrincipal user,
			List<URI> permitted) {
		// Ignore
	}
}
