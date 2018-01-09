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

import javax.annotation.Nonnull;

/**
 * The interface supported by all notification message dispatchers.
 * @author Donal Fellows
 */
public interface MessageDispatcher {
	/**
	 * @return Whether this message dispatcher is actually available (fully
	 *         configured, etc.)
	 */
	boolean isAvailable();

	/**
	 * @return The name of this dispatcher, which must match the protocol
	 *         supported by it (for a non-universal dispatcher) and the name of
	 *         the message generator used to produce the message.
	 */
	String getName();

	/**
	 * Dispatch a message to a recipient.
	 * 
	 * @param originator
	 *            The workflow run that produced the message.
	 * @param messageSubject
	 *            The subject of the message to send.
	 * @param messageContent
	 *            The plain-text content of the message to send.
	 * @param targetParameter
	 *            A description of where it is to go.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	void dispatch(@Nonnull TavernaRun originator,
			@Nonnull String messageSubject, @Nonnull String messageContent,
			@Nonnull String targetParameter) throws Exception;
}