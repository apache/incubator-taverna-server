/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.notAcceptable;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.taverna.server.master.rest.TavernaServerDirectoryREST.NegotiationFailedException;

@Provider
public class NegotiationFailedHandler implements
		ExceptionMapper<NegotiationFailedException> {
	@Override
	public Response toResponse(NegotiationFailedException exn) {
		return notAcceptable(exn.accepted).type(TEXT_PLAIN)
				.entity(exn.getMessage()).build();
	}
}
