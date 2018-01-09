/*
 */
package org.apache.taverna.server.master.exceptions;
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

import static org.apache.taverna.server.master.common.Namespaces.SERVER_SOAP;

import javax.xml.ws.WebFault;

/**
 * Some sort of exception that occurred which we can't map any other way. This
 * is generally indicative of a problem server-side.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "GeneralFailureFault", targetNamespace = SERVER_SOAP)
@SuppressWarnings("serial")
public class GeneralFailureException extends RuntimeException {
	public GeneralFailureException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public GeneralFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
