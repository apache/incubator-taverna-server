/*
 */
package org.taverna.server.localworker.remote;
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
 * States of a workflow run. They are {@link RemoteStatus#Initialized
 * Initialized}, {@link RemoteStatus#Operating Operating},
 * {@link RemoteStatus#Stopped Stopped}, and {@link RemoteStatus#Finished
 * Finished}. Conceptually, there is also a <tt>Destroyed</tt> state, but the
 * workflow run does not exist (and hence can't have its state queried or set)
 * in that case.
 * 
 * @author Donal Fellows
 */
public enum RemoteStatus {
	/**
	 * The workflow run has been created, but is not yet running. The run will
	 * need to be manually moved to {@link #Operating} when ready.
	 */
	Initialized,
	/**
	 * The workflow run is going, reading input, generating output, etc. Will
	 * eventually either move automatically to {@link #Finished} or can be moved
	 * manually to {@link #Stopped} (where supported).
	 */
	Operating,
	/**
	 * The workflow run is paused, and will need to be moved back to
	 * {@link #Operating} manually.
	 */
	Stopped,
	/**
	 * The workflow run has ceased; data files will continue to exist until the
	 * run is destroyed (which may be manual or automatic).
	 */
	Finished
}
