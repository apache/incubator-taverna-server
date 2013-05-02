/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

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
}
