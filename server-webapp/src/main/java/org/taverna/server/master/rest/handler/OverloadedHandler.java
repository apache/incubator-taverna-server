/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.OverloadedException;

@Provider
public class OverloadedHandler extends HandlerCore implements
		ExceptionMapper<OverloadedException> {
	@Override
	public Response toResponse(OverloadedException exn) {
		return respond(SERVICE_UNAVAILABLE, exn);
	}
}