package org.taverna.server.localworker.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.Principal;

/**
 * The main RMI-enabled interface for creating runs.
 * 
 * @author Donal Fellows
 */
public interface RemoteRunFactory extends Remote {
	/**
	 * Makes a workflow run that will process a particular workflow document.
	 * 
	 * @param workflow
	 *            The (serialized) workflow to instantiate as a run.
	 * @return A remote handle for the run.
	 */
	public RemoteSingleRun make(String workflow, Principal p)
			throws RemoteException;

	/**
	 * Asks this factory to unregister itself from the registry and cease
	 * operation.
	 */
	public void shutdown() throws RemoteException;
}
