/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.taverna.server.master.exceptions.NotOwnerException;

public class NotOwnerHandler extends HandlerCore implements
		ExceptionMapper<NotOwnerException> {
	@Override
	public Response toResponse(NotOwnerException exn) {
		return respond(FORBIDDEN, exn);
	}
}
