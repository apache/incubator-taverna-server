package org.taverna.server.localworker.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This represents the assignment of inputs to input ports of the workflow. Note
 * that the <tt>file</tt> and <tt>value</tt> properties are never set at the
 * same time.
 * 
 * @author Donal Fellows
 */
public interface RemoteInput extends Remote {
	/**
	 * @return The file currently assigned to this input port, or <tt>null</tt>
	 *         if no file is assigned.
	 */
	public String getFile() throws RemoteException;

	/**
	 * @return The name of this input port. This may not be changed.
	 */
	public String getName() throws RemoteException;

	/**
	 * @return The value currently assigned to this input port, or <tt>null</tt>
	 *         if no value is assigned.
	 */
	public String getValue() throws RemoteException;

	/**
	 * Sets the file to use for this input. This overrides the use of the
	 * previous file and any set value.
	 * 
	 * @param file
	 *            The filename to use. Must not start with a <tt>/</tt> or
	 *            contain any <tt>..</tt> segments. Will be interpreted relative
	 *            to the run's working directory.
	 */
	public void setFile(String file) throws RemoteException;

	/**
	 * Sets the value to use for this input. This overrides the use of the
	 * previous value and any set file.
	 * 
	 * @param value
	 *            The value to use.
	 */
	public void setValue(String value) throws RemoteException;
}
