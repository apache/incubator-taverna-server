/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import java.net.URI;
import java.util.List;

import org.taverna.server.master.common.Status;

/**
 * The worker policy delegates certain limits to the state model of the
 * particular worker.
 * 
 * @author Donal Fellows
 */
public interface PolicyLimits {
	/**
	 * @return the maximum number of extant workflow runs in any state
	 */
	int getMaxRuns();

	/**
	 * @return the maximum number of workflow runs in the
	 *         {@linkplain Status#Operating operating} state.
	 */
	int getOperatingLimit();

	/**
	 * @return the list of URIs to workflows that may be used to create workflow
	 *         runs. If empty or <tt>null</tt>, no restriction is present.
	 */
	List<URI> getPermittedWorkflowURIs();

	/**
	 * @param permitted
	 *            the list of URIs to workflows that may be used to create
	 *            workflow runs.
	 */
	void setPermittedWorkflowURIs(List<URI> permitted);
}
