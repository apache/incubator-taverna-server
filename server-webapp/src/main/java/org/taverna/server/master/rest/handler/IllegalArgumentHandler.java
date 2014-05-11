/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentHandler extends HandlerCore implements
		ExceptionMapper<IllegalArgumentException> {
	@Override
	public Response toResponse(IllegalArgumentException exn) {
		return respond(UNSUPPORTED_MEDIA_TYPE, exn);
	}
}
