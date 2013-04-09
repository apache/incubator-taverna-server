/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.springframework.security.access.AccessDeniedException;

public class AccessDeniedHandler extends HandlerCore implements
		ExceptionMapper<AccessDeniedException> {
	@Override
	public Response toResponse(AccessDeniedException exception) {
		return respond(FORBIDDEN, exception);
	}
}
