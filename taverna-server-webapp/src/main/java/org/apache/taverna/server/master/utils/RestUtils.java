/*
 */
package org.apache.taverna.server.master.utils;
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

import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Response;

/**
 * Utilities that make it easier to write REST services.
 * 
 * @author Donal Fellows
 */
public class RestUtils {
	/**
	 * Generate a response to an HTTP OPTIONS request.
	 * 
	 * @param methods
	 *            The state-changing methods supported, if any.
	 * @return the required response
	 * @see OPTIONS
	 */
	public static Response opt(String... methods) {
		StringBuilder sb = new StringBuilder("GET,");
		for (String m : methods)
			sb.append(m).append(",");
		sb.append("HEAD,OPTIONS");
		return Response.ok().header("Allow", sb.toString()).entity("").build();
	}
}
