/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import org.taverna.server.master.common.Status;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;

/**
 * This represents the assignment of inputs to input ports of the workflow. Note
 * that the <tt>file</tt> and <tt>value</tt> properties are never set at the
 * same time.
 * 
 * @author Donal Fellows
 */
public interface Input {
	/**
	 * @return The file currently assigned to this input port, or <tt>null</tt>
	 *         if no file is assigned.
	 */
	public String getFile();

	/**
	 * @return The name of this input port. This may not be changed.
	 */
	public String getName();

	/**
	 * @return The value currently assigned to this input port, or <tt>null</tt>
	 *         if no value is assigned.
	 */
	public String getValue();

	/**
	 * Sets the file to use for this input. This overrides the use of the
	 * previous file and any set value.
	 * 
	 * @param file
	 *            The filename to use. Must not start with a <tt>/</tt> or
	 *            contain any <tt>..</tt> segments. Will be interpreted relative
	 *            to the run's working directory.
	 * @throws FilesystemAccessException
	 *             If the filename is invalid.
	 * @throws BadStateChangeException
	 *             If the run isn't in the {@link Status#Initialized
	 *             Initialized} state.
	 */
	public void setFile(String file) throws FilesystemAccessException,
			BadStateChangeException;

	/**
	 * Sets the value to use for this input. This overrides the use of the
	 * previous value and any set file.
	 * 
	 * @param value
	 *            The value to use.
	 * @throws BadStateChangeException
	 *             If the run isn't in the {@link Status#Initialized
	 *             Initialized} state.
	 */
	public void setValue(String value) throws BadStateChangeException;
}
