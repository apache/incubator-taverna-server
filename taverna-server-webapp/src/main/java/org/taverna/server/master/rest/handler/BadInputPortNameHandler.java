/*
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.BadInputPortNameException;

@Provider
public class BadInputPortNameHandler extends HandlerCore implements
		ExceptionMapper<BadInputPortNameException> {
	@Override
	public Response toResponse(BadInputPortNameException exn) {
		return respond(NOT_FOUND, exn);
	}
}