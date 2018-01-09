/*
 */
package org.apache.taverna.server.master.worker;
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


/**
 * How to convert a notification about the completion of a job into a message.
 * 
 * @author Donal Fellows
 */
public interface CompletionNotifier {
	/**
	 * @return The name of this notifier.
	 */
	String getName();

	/**
	 * Called to get the content of a message that a workflow run has finished.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param run
	 *            What run are we talking about.
	 * @param code
	 *            What the exit code was.
	 * @return The plain-text content of the message.
	 */
	String makeCompletionMessage(String name, RemoteRunDelegate run, int code);

	/**
	 * Called to get the subject of the message to dispatch.
	 * 
	 * @param name
	 *            The name of the run.
	 * @param run
	 *            What run are we talking about.
	 * @param code
	 *            What the exit code was.
	 * @return The plain-text subject of the message.
	 */
	String makeMessageSubject(String name, RemoteRunDelegate run, int code);
}
