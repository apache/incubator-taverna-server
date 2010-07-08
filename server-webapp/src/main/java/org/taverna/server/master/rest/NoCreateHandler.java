/**
 * 
 */
package org.taverna.server.master.rest;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.taverna.server.master.TavernaServerImpl.log;
import static org.taverna.server.master.TavernaServerImpl.logOutgoingExceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.exceptions.NoCreateException;

@Provider
public class NoCreateHandler implements ExceptionMapper<NoCreateException> {
	@Override
	public Response toResponse(NoCreateException exn) {
		if (logOutgoingExceptions)
			log.info("converting exception to response", exn);
		return Response.status(FORBIDDEN).type(TEXT_PLAIN_TYPE).entity(
				exn.getMessage()).build();
	}
}