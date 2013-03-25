/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.localworker.server.UsageRecordReceiver;

/**
 * The interface between the connectivity layer and the thunk to the
 * subprocesses.
 * 
 * @author Donal Fellows
 */
public interface Worker {
	/**
	 * Fire up the workflow. This causes a transition into the operating state.
	 * 
	 * @param local
	 *            The reference to the factory class for this worker.
	 * @param executeWorkflowCommand
	 *            The command to run to execute the workflow.
	 * @param workflow
	 *            The workflow document to execute.
	 * @param workingDir
	 *            What directory to use as the working directory.
	 * @param inputBaclavaFile
	 *            The baclava file to use for inputs, or <tt>null</tt> to use
	 *            the other <b>input*</b> arguments' values.
	 * @param inputRealFiles
	 *            A mapping of input names to files that supply them. Note that
	 *            we assume that nothing mapped here will be mapped in
	 *            <b>inputValues</b>.
	 * @param inputValues
	 *            A mapping of input names to values to supply to them. Note
	 *            that we assume that nothing mapped here will be mapped in
	 *            <b>inputFiles</b>.
	 * @param outputBaclavaFile
	 *            What baclava file to write the output from the workflow into,
	 *            or <tt>null</tt> to have it written into the <tt>out</tt>
	 *            subdirectory.
	 * @param contextDirectory
	 *            The directory containing the keystore and truststore. <i>Must
	 *            not be <tt>null</tt>.</i>
	 * @param keystorePassword
	 *            The password to the keystore and truststore. <i>Must not be
	 *            <tt>null</tt>.</i>
	 * @param environment
	 *            Any environment variables that need to be added to the
	 *            invokation.
	 * @param masterToken
	 *            The internal name of the workflow run.
	 * @param runtimeSettings
	 *            List of configuration details for the forked runtime.
	 * @return Whether a successful start happened.
	 * @throws Exception
	 *             If any of quite a large number of things goes wrong.
	 */
	boolean initWorker(LocalWorker local, String executeWorkflowCommand,
			String workflow, File workingDir, File inputBaclavaFile,
			Map<String, File> inputRealFiles, Map<String, String> inputValues,
			File outputBaclavaFile, File contextDirectory,
			char[] keystorePassword, Map<String, String> environment,
			String masterToken, List<String> runtimeSettings) throws Exception;

	/**
	 * Kills off the subprocess if it exists and is alive.
	 * 
	 * @throws Exception
	 *             if anything goes badly wrong when the worker is being killed
	 *             off.
	 */
	void killWorker() throws Exception;

	/**
	 * Move the worker out of the stopped state and back to operating.
	 * 
	 * @throws Exception
	 *             if it fails (which it always does; operation currently
	 *             unsupported).
	 */
	void startWorker() throws Exception;

	/**
	 * Move the worker into the stopped state from the operating state.
	 * 
	 * @throws Exception
	 *             if it fails (which it always does; operation currently
	 *             unsupported).
	 */
	void stopWorker() throws Exception;

	/**
	 * @return The status of the workflow run. Note that this can be an
	 *         expensive operation.
	 */
	RemoteStatus getWorkerStatus();

	/**
	 * @return The listener that is registered by default, in addition to all
	 *         those that are explicitly registered by the user.
	 */
	RemoteListener getDefaultListener();

	/**
	 * @param receiver
	 *            The destination where any final usage records are to be
	 *            written in order to log them back to the server.
	 */
	void setURReceiver(UsageRecordReceiver receiver);

	/**
	 * Arrange for the deletion of any resources created during worker process
	 * construction. Guaranteed to be the last thing done before finalization.
	 * 
	 * @throws ImplementationException
	 *             If anything goes wrong.
	 */
	void deleteLocalResources() throws ImplementationException;
}
