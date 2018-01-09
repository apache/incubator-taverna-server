/*
 */
package org.apache.taverna.server.master.factories;
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

import java.util.List;

import org.apache.taverna.server.master.exceptions.NoListenerException;
import org.apache.taverna.server.master.interfaces.Listener;
import org.apache.taverna.server.master.interfaces.TavernaRun;

/**
 * How to make event listeners of various types that are attached to a workflow
 * instance.
 * 
 * @author Donal Fellows
 */
public interface ListenerFactory {
	/**
	 * Make an event listener.
	 * 
	 * @param run
	 *            The workflow instance to attach the event listener to.
	 * @param listenerType
	 *            The type of event listener to create. Must be one of the
	 *            strings returned by {@link #getSupportedListenerTypes()}.
	 * @param configuration
	 *            A configuration document to pass to the listener.
	 * @return The event listener that was created.
	 * @throws NoListenerException
	 *             If the <b>listenerType</b> is unrecognized or the
	 *             <b>configuration</b> is bad in some way.
	 */
	public Listener makeListener(TavernaRun run, String listenerType,
			String configuration) throws NoListenerException;

	/**
	 * What types of listener are supported? Note that we assume that the list
	 * of types is the same for all users and all workflow instances.
	 * 
	 * @return A list of supported listener types.
	 */
	public List<String> getSupportedListenerTypes();
}
