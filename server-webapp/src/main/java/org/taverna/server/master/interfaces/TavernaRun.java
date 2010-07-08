package org.taverna.server.master.interfaces;

import java.util.Date;
import java.util.List;

import org.taverna.server.master.common.SCUFL;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDestroyException;

/**
 * The interface to a taverna workflow run, or "run" for short.
 * 
 * @author Donal Fellows
 */
public interface TavernaRun {
	/**
	 * @return What was this run was create to execute.
	 */
	public SCUFL getWorkflow();

	/**
	 * @return The name of the Baclava file to use for all inputs, or
	 *         <tt>null</tt> if no Baclava file is set.
	 */
	public String getInputBaclavaFile();

	/**
	 * Sets the Baclava file to use for all inputs. This overrides the use of
	 * individual inputs.
	 * 
	 * @param filename
	 *            The filename to use. Must not start with a <tt>/</tt> or
	 *            contain any <tt>..</tt> segments. Will be interpreted relative
	 *            to the run's working directory.
	 * @throws FilesystemAccessException
	 *             If the filename is invalid.
	 * @throws BadStateChangeException
	 *             If the workflow is not in the {@link Status#Initialized
	 *             Initialized} state.
	 */
	public void setInputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException;

	/**
	 * @return The list of input assignments.
	 */
	public List<Input> getInputs();

	/**
	 * Create an input assignment.
	 * 
	 * @param name
	 *            The name of the port that this will be an input for.
	 * @return The assignment reference.
	 * @throws BadStateChangeException
	 *             If the workflow is not in the {@link Status#Initialized
	 *             Initialized} state.
	 */
	public Input makeInput(String name) throws BadStateChangeException;

	/**
	 * @return The file (relative to the working directory) to write the outputs
	 *         of the run to as a Baclava document, or <tt>null</tt> if they are
	 *         to be written to non-Baclava files in a directory called
	 *         <tt>out</tt>.
	 */
	public String getOutputBaclavaFile();

	/**
	 * Sets where the output of the run is to be written to. This will cause the
	 * output to be generated as a Baclava document, rather than a collection of
	 * individual non-Baclava files in the subdirectory of the working directory
	 * called <tt>out</tt>.
	 * 
	 * @param filename
	 *            Where to write the Baclava file (or <tt>null</tt> to cause the
	 *            output to be written to individual files); overwrites any
	 *            previous setting of this value.
	 * @throws FilesystemAccessException
	 *             If the filename starts with a <tt>/</tt> or contains a
	 *             <tt>..</tt> segment.
	 * @throws BadStateChangeException
	 *             If the workflow is not in the {@link Status#Initialized
	 *             Initialized} state.
	 */
	public void setOutputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException;

	/**
	 * @return When this run will expire, becoming eligible for automated
	 *         deletion.
	 */
	public Date getExpiry();

	/**
	 * Set when this run will expire.
	 * 
	 * @param d
	 *            Expiry time. Deletion will happen some time after that.
	 */
	public void setExpiry(Date d);

	/**
	 * @return The current status of the run.
	 */
	public Status getStatus();

	/**
	 * Set the status of the run, which should cause it to move into the given
	 * state. This may cause some significant changes.
	 * 
	 * @param s
	 *            The state to try to change to.
	 * @throws BadStateChangeException
	 *             If the change to the given state is impossible.
	 */
	public void setStatus(Status s) throws BadStateChangeException;

	/**
	 * @return Handle to the main working directory of the run.
	 * @throws FilesystemAccessException
	 */
	public Directory getWorkingDirectory() throws FilesystemAccessException;

	/**
	 * @return The list of listener instances attached to the run.
	 */
	public List<Listener> getListeners();

	/**
	 * Add a listener to the run.
	 * 
	 * @param listener
	 *            The listener to add.
	 */
	public void addListener(Listener listener);

	/**
	 * @return The security context structure for this run.
	 */
	public TavernaSecurityContext getSecurityContext();

	/**
	 * Kill off this run, removing all resources which it consumes.
	 * 
	 * @throws NoDestroyException
	 *             If the destruction failed.
	 */
	public void destroy() throws NoDestroyException;
}
