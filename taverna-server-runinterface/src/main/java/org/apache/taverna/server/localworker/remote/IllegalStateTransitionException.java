/*
 */
package org.apache.taverna.server.localworker.remote;
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

import javax.xml.ws.WebFault;

/**
 * Exception that indicates where a change of a workflow run's status is
 * illegal.
 * 
 * @author Donal Fellows
 * @see RemoteSingleRun#setStatus(RemoteStatus)
 */
@WebFault(name = "IllegalStateTransitionFault", targetNamespace = "http://ns.taverna.org.uk/2010/xml/server/worker/")
public class IllegalStateTransitionException extends Exception {
	private static final long serialVersionUID = 159673249162345L;

	public IllegalStateTransitionException() {
		this("illegal state transition");
	}

	public IllegalStateTransitionException(String message) {
		super(message);
	}

	public IllegalStateTransitionException(Throwable cause) {
		this("illegal state transition", cause);
	}

	public IllegalStateTransitionException(String message, Throwable cause) {
		super(message, cause);
	}
}
