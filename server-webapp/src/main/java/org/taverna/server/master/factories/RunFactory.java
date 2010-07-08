package org.taverna.server.master.factories;

import java.security.Principal;

import org.taverna.server.master.common.SCUFL;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.interfaces.TavernaRun;

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
	 * @throws NoCreateException On failure.
	 */
	public TavernaRun create(Principal creator, SCUFL workflow) throws NoCreateException;
}
