package org.taverna.server.master.rest;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.NoCreateException;

@Provider
public class NoCreateHandler extends HandlerCore implements
		ExceptionMapper<NoCreateException> {
	@Override
	public Response toResponse(NoCreateException exn) {
		return respond(FORBIDDEN, exn);
	}
}