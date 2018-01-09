/*
 */
package org.apache.taverna.server.master.interfaces;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.taverna.server.master.exceptions.BadPropertyValueException;
import org.apache.taverna.server.master.exceptions.NoListenerException;

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
