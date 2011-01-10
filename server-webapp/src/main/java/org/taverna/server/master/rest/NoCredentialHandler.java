/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.rest;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.taverna.server.master.exceptions.NoCredentialException;

public class NoCredentialHandler extends HandlerCore implements
		ExceptionMapper<NoCredentialException> {
	@Override
	public Response toResponse(NoCredentialException exn) {
		return respond(NOT_FOUND, exn);
	}
}
