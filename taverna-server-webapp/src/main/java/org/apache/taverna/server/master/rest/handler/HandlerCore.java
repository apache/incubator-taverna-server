/*
 */
package org.apache.taverna.server.master.rest.handler;
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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.logging.LogFactory.getLog;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.taverna.server.master.api.ManagementModel;

/**
 * Base class for handlers that grants Spring-enabled access to the management
 * model.
 * 
 * @author Donal Fellows
 */
public class HandlerCore {
	private Log log = getLog("Taverna.Server.Webapp");
	private ManagementModel managementModel;

	/**
	 * @param managementModel
	 *            the managementModel to set
	 */
	public void setManagementModel(ManagementModel managementModel) {
		this.managementModel = managementModel;
	}

	/**
	 * Simplified interface for building responses.
	 * 
	 * @param status
	 *            What status code to use?
	 * @param exception
	 *            What exception to report on?
	 * @return The build response.
	 */
	protected Response respond(Response.Status status, Exception exception) {
		if (managementModel.getLogOutgoingExceptions()
				|| status.getStatusCode() >= 500)
			log.info("converting exception to response", exception);
		return status(status).type(TEXT_PLAIN_TYPE)
				.entity(exception.getMessage()).build();
	}

	/**
	 * Simplified interface for building responses.
	 * 
	 * @param status
	 *            What status code to use?
	 * @param partialMessage
	 *            The prefix to the message.
	 * @param exception
	 *            What exception to report on?
	 * @return The build response.
	 */
	protected Response respond(Response.Status status, String partialMessage,
			Exception exception) {
		if (managementModel.getLogOutgoingExceptions()
				|| status.getStatusCode() >= 500)
			log.info("converting exception to response", exception);
		return status(status).type(TEXT_PLAIN_TYPE)
				.entity(partialMessage + "\n" + exception.getMessage()).build();
	}
}
