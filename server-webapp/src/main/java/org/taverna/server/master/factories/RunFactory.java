/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.factories;

import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * How to construct a Taverna Server Workflow Run.
 * 
 * @author Donal Fellows
 */
public interface RunFactory {
	/**
	 * Make a Taverna Server workflow run that is bound to a particular user
	 * (the "creator") and able to run a particular workflow.
	 * 
	 * @param creator
	 *            The user creating the workflow instance.
	 * @param workflow
	 *            The workflow to instantiate
	 * @return An object representing the run.
	 * @throws NoCreateException
	 *             On failure.
	 */
	TavernaRun create(UsernamePrincipal creator, Workflow workflow)
			throws NoCreateException;

	/**
	 * Check whether the factory is permitting runs to actually start operating.
	 * 
	 * @return Whether a run should start.
	 */
	boolean isAllowingRunsToStart();
}
