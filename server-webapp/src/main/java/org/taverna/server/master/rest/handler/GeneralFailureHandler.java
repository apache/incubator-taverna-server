/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.taverna.server.master.exceptions.GeneralFailureException;

public class GeneralFailureHandler extends HandlerCore implements
		ExceptionMapper<GeneralFailureException> {
	@Override
	public Response toResponse(GeneralFailureException exception) {
		return respond(INTERNAL_SERVER_ERROR, exception);
	}
}
