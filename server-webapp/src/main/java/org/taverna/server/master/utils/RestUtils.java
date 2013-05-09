/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

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
		return Response.ok().header("Accept", sb.toString()).entity("").build();
	}
}
