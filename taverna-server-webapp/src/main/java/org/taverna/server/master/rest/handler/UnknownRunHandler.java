/*
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.UnknownRunException;

@Provider
public class UnknownRunHandler extends HandlerCore implements
		ExceptionMapper<UnknownRunException> {
	@Override
	public Response toResponse(UnknownRunException exn) {
		return respond(NOT_FOUND, exn);
	}
}