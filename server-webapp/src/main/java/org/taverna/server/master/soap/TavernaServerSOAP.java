package org.taverna.server.master.soap;

import static org.taverna.server.master.common.Namespaces.SERVER_SOAP;

import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.common.InputDescription;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.SCUFL;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.rest.TavernaServerREST;

/**
 * The SOAP service interface to Taverna Server version 2.3.
 * 
 * @author Donal Fellows
 * @see TavernaServerREST
 */
@WebService(name = "tavernaService", targetNamespace = SERVER_SOAP)
public interface TavernaServerSOAP {
	/**
	 * Make a run for a particular workflow.
	 * 
	 * @param workflow
	 *            The workflow to instantiate.
	 * @return Annotated handle for created run.
	 * @throws NoUpdateException
	 */
	@WebResult(name = "Run")
	RunReference submitWorkflow(@WebParam(name = "workflow") SCUFL workflow)
			throws NoUpdateException;

	/**
	 * Get the list of existing runs owned by the user.
	 * 
	 * @return Annotated handle list.
	 */
	@WebResult(name = "Run")
	RunReference[] listRuns();

	/**
	 * Get the upper limit on the number of runs that the user may create at
	 * once.
	 * 
	 * @return The limit. <b>NB:</b> the number currently operating may be
	 *         larger, but in that case no further runs can be made until some
	 *         of the old ones are destroyed.
	 */
	@WebResult(name = "MaxSimultaneousRuns")
	public int getMaxSimultaneousRuns();

	/**
	 * Get the list of allowed workflows. If the list is empty, <i>any</i>
	 * workflow may be used.
	 * 
	 * @return A list of SCUFL workflows.
	 */
	@WebMethod(operationName = "getPermittedWorkflows")
	@WebResult(name = "PermittedWorkflow")
	public SCUFL[] getAllowedWorkflows();

	/**
	 * Get the list of allowed event listeners.
	 * 
	 * @return A list of listener names.
	 */
	@WebMethod(operationName = "getPermittedListenerTypes")
	@WebResult(name = "PermittedListenerType")
	public String[] getAllowedListeners();

	/**
	 * Destroy a run immediately. This might or might not actually relinquish
	 * resources; that's up to the service implementation and deployment. This
	 * does <i>not</i> remove any entries associated with the handle in the
	 * provenance database.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the lifetime of the
	 *             run.
	 */
	public void destroyRun(@WebParam(name = "runName") String runName)
			throws UnknownRunException, NoUpdateException;

	/**
	 * Get the workflow document used to create the given run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The workflow document.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "CreationWorkflow")
	public SCUFL getRunWorkflow(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Get the description of the inputs to the workflow run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The input description
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "RunInputDescription")
	public InputDescription getRunInputs(
			@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Tells the run to use the given Baclava file for all inputs.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param fileName
	 *            The name of the file to use. Must not start with a <tt>/</tt>
	 *            or contain a <tt>..</tt> element.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws FilesystemAccessException
	 *             If the filename is illegal.
	 * @throws BadStateChangeException
	 *             If the run is not in the {@link Status#Initialized
	 *             Initialized} state
	 */
	public void setRunInputBaclavaFile(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "fileName") String fileName)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException;

	/**
	 * Tells the run to use the given file for input on the given port. This
	 * overrides any previously set file or value on the port and causes the
	 * server to forget about using a Baclava file for all inputs.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param portName
	 *            The port to use the file for.
	 * @param portFilename
	 *            The file to use on the port. Must not start with a <tt>/</tt>
	 *            or contain a <tt>..</tt> element.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws FilesystemAccessException
	 *             If the filename is illegal.
	 * @throws BadStateChangeException
	 *             If the run is not in the {@link Status#Initialized
	 *             Initialized} state.
	 * @throws BadPropertyValueException
	 *             If the input port may not be changed to the contents of the
	 *             given file.
	 */
	public void setRunInputPortFile(@WebParam(name = "runName") String runName,
			@WebParam(name = "portName") String portName,
			@WebParam(name = "portFilename") String portFilename)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException,
			BadPropertyValueException;

	/**
	 * Tells the run to use the given value for input on the given port. This
	 * overrides any previously set file or value on the port and causes the
	 * server to forget about using a Baclava file for all inputs. Note that
	 * this is wholly unsuitable for use with binary data.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param portName
	 *            The port to use the file for.
	 * @param portValue
	 *            The literal value to use on the port.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws BadStateChangeException
	 *             If the run is not in the {@link Status#Initialized
	 *             Initialized} state.
	 * @throws BadPropertyValueException
	 *             If the input port may not be changed to the given literal
	 *             value.
	 */
	public void setRunInputPortValue(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "portName") String portName,
			@WebParam(name = "portValue") String portValue)
			throws UnknownRunException, NoUpdateException,
			BadStateChangeException, BadPropertyValueException;

	/**
	 * Get the Baclava file where the output of the run will be written.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The filename, or <tt>null</tt> if the results will be written to
	 *         a subdirectory <tt>out</tt> of the run's working directory.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "OutputBaclavaFile")
	public String getRunOutputBaclavaFile(
			@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Set the Baclava file where the output of the run will be written.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param outputFile
	 *            The filename for the Baclava file, or <tt>null</tt> or the
	 *            empty string to indicate that the results are to be written to
	 *            the subdirectory <tt>out</tt> of the run's working directory.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws FilesystemAccessException
	 *             If the filename is illegal (starts with a <tt>/</tt> or
	 *             contains a <tt>..</tt> element.
	 * @throws BadStateChangeException
	 *             If the run is not in the {@link Status#Initialized
	 *             Initialized} state
	 */
	public void setRunOutputBaclavaFile(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "outputFile") String outputFile)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException, BadStateChangeException;

	/**
	 * Get the time when the run will be eligible to be automatically deleted.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return A date at which the expiry will be scheduled. The actual deletion
	 *         will happen an arbitrary amount of time later (depending on
	 *         system policy).
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "Expiry")
	public Date getRunExpiry(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Set when the run will be eligible to be automatically deleted.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param expiry
	 *            A date at which the expiry will be scheduled. The actual
	 *            deletion will happen an arbitrary amount of time later
	 *            (depending on system policy).
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the lifetime of the
	 *             run.
	 */
	public void setRunExpiry(@WebParam(name = "runName") String runName,
			@WebParam(name = "expiry") Date expiry) throws UnknownRunException,
			NoUpdateException;

	/**
	 * Get the time when the run was created.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The moment when the run was created (modulo some internal
	 *         overhead).
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "CreationTime")
	public Date getRunCreationTime(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Get the time when the run was started.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The moment when the run was started (modulo some internal
	 *         overhead) or <tt>null</tt> to indicate that it has never started.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "StartTime")
	public Date getRunStartTime(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Get the time when the run was detected as having finished.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The moment when the run was believed stopped. Note that this may
	 *         not be when the run <i>actually</i> finished; promptness of
	 *         detection depends on many factors.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "FinishTime")
	public Date getRunFinishTime(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Get the current status of the run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The status code.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "Status")
	public Status getRunStatus(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Set the status of a run. This is used to start it executing, make it stop
	 * executing, etc. Note that changing the status of a run can <i>never</i>
	 * cause the run to be destroyed.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param status
	 *            The status to change to. Changing to the current status will
	 *            always have no effect.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws BadStateChangeException
	 *             If the state change requested is impossible.
	 */
	public void setRunStatus(@WebParam(name = "runName") String runName,
			@WebParam(name = "status") Status status)
			throws UnknownRunException, NoUpdateException,
			BadStateChangeException;

	/**
	 * Get the names of the event listeners attached to the run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The listener names.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "ListenerName")
	public String[] getRunListeners(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Adds an event listener to the run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param listenerType
	 *            The type of event listener to add. Must be one of the names
	 *            returned by the {@link #getAllowedListeners()} operation.
	 * @param configuration
	 *            The configuration document for the event listener; the
	 *            interpretation of the configuration is up to the listener.
	 * @return The actual name of the listener.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to manipulate the run.
	 * @throws NoListenerException
	 *             If the listener construction fails (<i>e.g.</i>, due to an
	 *             unsupported <b>listenerType</b> or a problem with the
	 *             <b>configuration</b>).
	 */
	@WebResult(name = "ListenerName")
	public String addRunListener(@WebParam(name = "runName") String runName,
			@WebParam(name = "listenerType") String listenerType,
			@WebParam(name = "configuration") String configuration)
			throws UnknownRunException, NoUpdateException, NoListenerException;

	/**
	 * Get the owner of the run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @return The status code.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 */
	@WebResult(name = "Owner")
	public String getRunOwner(@WebParam(name = "runName") String runName)
			throws UnknownRunException;

	/**
	 * Get the contents of any directory at/under the run's working directory.
	 * Runs do not share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param directory
	 *            The name of the directory to fetch; the main working directory
	 *            is <tt>/</tt> and <tt>..</tt> is always disallowed.
	 * @return A list of entries. They are assumed to be all directories or
	 *         files.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., reading the contents of
	 *             a file).
	 */
	@WebResult(name = "DirectoryEntry")
	public DirEntryReference[] getRunDirectoryContents(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "directory") DirEntryReference directory)
			throws UnknownRunException, FilesystemAccessException;

	/**
	 * Get the contents of any directory (and its subdirectories) at/under the
	 * run's working directory, returning it as a compressed ZIP file. Runs do
	 * not share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param directory
	 *            The name of the directory to fetch; the main working directory
	 *            is <tt>/</tt> and <tt>..</tt> is always disallowed.
	 * @return A serialized ZIP file.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., reading the contents of
	 *             a file).
	 */
	@WebResult(name = "ZipFile")
	public byte[] getRunDirectoryAsZip(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "directory") DirEntryReference directory)
			throws UnknownRunException, FilesystemAccessException;

	/**
	 * Make a new empty directory beneath an existing one, which must be the
	 * run's working directory or a directory beneath it. Runs do not share
	 * working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param parent
	 *            The parent directory that will have the new directory added
	 *            beneath it.
	 * @param name
	 *            The name of the directory to create. Must not be the same as
	 *            any other file or directory in the <i>parent</i> directory.
	 *            The name <i>must not</i> consist of <tt>..</tt> or have a
	 *            <tt>/</tt> in it.
	 * @return A reference to the created directory.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user is not allowed to make modifications to the run.
	 * @throws FilesystemAccessException
	 *             If some assuption is violated (e.g., making something with
	 *             the same name as something that already exists).
	 */
	@WebResult(name = "CreatedDirectory")
	public DirEntryReference makeRunDirectory(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "parentDirectory") DirEntryReference parent,
			@WebParam(name = "directoryName") String name)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException;

	/**
	 * Make a new empty file in an existing directory, which may be the run's
	 * working directory or any directory beneath it. Runs do not share working
	 * directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param parent
	 *            The parent directory that will have the new file added to it.
	 * @param name
	 *            The name of the file to create. Must not be the same as any
	 *            other file or directory in the <i>parent</i> directory. The
	 *            name <i>must not</i> consist of <tt>..</tt> or have a
	 *            <tt>/</tt> in it.
	 * @return A reference to the created file. The file will be empty.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user is not allowed to make modifications to the run.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., making something with
	 *             the same name as something that already exists).
	 */
	@WebResult(name = "CreatedFile")
	public DirEntryReference makeRunFile(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "parentDirectory") DirEntryReference parent,
			@WebParam(name = "fileName") String name)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException;

	/**
	 * Destroy an entry (file or directory) in or beneath a run's working
	 * directory. Runs do not share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param dirEntry
	 *            Reference to an existing item in a directory that will be
	 *            destroyed. May be a reference to either a file or a directory.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user is not allowed to make modifications to the run.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., deleting something
	 *             which doesn't exist or attempting to delete the main working
	 *             directory).
	 */
	public void destroyRunDirectoryEntry(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "directoryEntry") DirEntryReference dirEntry)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException;

	/**
	 * Get the contents of a file under the run's working directory. Runs do not
	 * share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param file
	 *            The name of the file to fetch; the main working directory is
	 *            <tt>/</tt> and <tt>..</tt> is always disallowed.
	 * @return The literal byte contents of the file.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., reading the contents of
	 *             a directory).
	 */
	@WebResult(name = "FileContents")
	public byte[] getRunFileContents(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "fileName") DirEntryReference file)
			throws UnknownRunException, FilesystemAccessException;

	/**
	 * Set the contents of a file under the run's working directory. Runs do not
	 * share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param file
	 *            The name of the file to update; the main working directory is
	 *            <tt>/</tt> and <tt>..</tt> is always disallowed.
	 * @param newContents
	 *            The literal bytes to set the file contents to.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoUpdateException
	 *             If the user is not allowed to make modifications to the run.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., writing the contents of
	 *             a directory).
	 */
	public void setRunFileContents(@WebParam(name = "runName") String runName,
			@WebParam(name = "fileName") DirEntryReference file,
			@WebParam(name = "contents") byte[] newContents)
			throws UnknownRunException, NoUpdateException,
			FilesystemAccessException;

	/**
	 * Get the length of any file (in bytes) at/under the run's working
	 * directory. Runs do not share working directories.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param file
	 *            The name of the file to get the length of; the main working
	 *            directory is <tt>/</tt> and <tt>..</tt> is always disallowed.
	 * @return The number of bytes in the file.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws FilesystemAccessException
	 *             If some assumption is violated (e.g., reading the length of a
	 *             directory).
	 */
	@WebResult(name = "FileLength")
	public long getRunFileLength(@WebParam(name = "runName") String runName,
			@WebParam(name = "fileName") DirEntryReference file)
			throws UnknownRunException, FilesystemAccessException;

	/**
	 * Get the configuration document for an event listener attached to a run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param listenerName
	 *            The name of the listener attached.
	 * @return The configuration document.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoListenerException
	 *             If no such listener exists.
	 */
	@WebResult(name = "ListenerConfiguration")
	public String getRunListenerConfiguration(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "listenerName") String listenerName)
			throws UnknownRunException, NoListenerException;

	/**
	 * Get the list of properties supported by an event listener attached to a
	 * run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param listenerName
	 *            The name of the listener attached.
	 * @return The list of property names.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoListenerException
	 *             If no such listener exists.
	 */
	@WebResult(name = "ListenerPropertyName")
	public String[] getRunListenerProperties(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "listenerName") String listenerName)
			throws UnknownRunException, NoListenerException;

	/**
	 * Get the value of a property for an event listener attached to a run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param listenerName
	 *            The name of the listener attached.
	 * @param propertyName
	 *            The name of the property to read.
	 * @return The configuration document.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoListenerException
	 *             If no such listener exists or if the listener has no such
	 *             property.
	 */
	@WebResult(name = "ListenerPropertyValue")
	public String getRunListenerProperty(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "listenerName") String listenerName,
			@WebParam(name = "propertyName") String propertyName)
			throws UnknownRunException, NoListenerException;

	/**
	 * Set the value of a property for an event listener attached to a run.
	 * 
	 * @param runName
	 *            The handle of the run.
	 * @param listenerName
	 *            The name of the listener attached.
	 * @param propertyName
	 *            The name of the property to write.
	 * @param value
	 *            The value to set the property to.
	 * @throws UnknownRunException
	 *             If the server doesn't know about the run or if the user is
	 *             not permitted to see it.
	 * @throws NoListenerException
	 *             If no such listener exists, the listener has no such
	 *             property, or the value is considered "unacceptable" in some
	 *             way.
	 * @throws NoUpdateException
	 *             If the user is not allowed to make modifications to the run.
	 */
	public void setRunListenerProperty(
			@WebParam(name = "runName") String runName,
			@WebParam(name = "listenerName") String listenerName,
			@WebParam(name = "propertName") String propertyName,
			@WebParam(name = "value") String value) throws UnknownRunException,
			NoUpdateException, NoListenerException;
}
