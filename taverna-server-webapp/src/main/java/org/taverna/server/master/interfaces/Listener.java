/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.NoListenerException;

/**
 * An event listener that can be attached to a {@link TavernaRun}.
 * 
 * @author Donal Fellows
 */
public interface Listener {
	/**
	 * @return The name of the listener.
	 */
	public String getName();

	/**
	 * @return The type of the listener.
	 */
	public String getType();

	/**
	 * @return The configuration document for the listener.
	 */
	public String getConfiguration();

	/**
	 * @return The supported properties of the listener.
	 */
	public String[] listProperties();

	/**
	 * Get the value of a particular property, which should be listed in the
	 * {@link #listProperties()} method.
	 * 
	 * @param propName
	 *            The name of the property to read.
	 * @return The value of the property.
	 * @throws NoListenerException
	 *             If no property with that name exists.
	 */
	public String getProperty(String propName) throws NoListenerException;

	/**
	 * Set the value of a particular property, which should be listed in the
	 * {@link #listProperties()} method.
	 * 
	 * @param propName
	 *            The name of the property to write.
	 * @param value
	 *            The value to set the property to.
	 * @throws NoListenerException
	 *             If no property with that name exists.
	 * @throws BadPropertyValueException
	 *             If the value of the property is bad (e.g., wrong syntax).
	 */
	public void setProperty(String propName, String value)
			throws NoListenerException, BadPropertyValueException;
}
