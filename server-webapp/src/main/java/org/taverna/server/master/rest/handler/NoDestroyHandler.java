/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.NoDestroyException;

@Provider
public class NoDestroyHandler extends HandlerCore implements
		ExceptionMapper<NoDestroyException> {
	@Override
	public Response toResponse(NoDestroyException exn) {
		return respond(FORBIDDEN, exn);
	}
}