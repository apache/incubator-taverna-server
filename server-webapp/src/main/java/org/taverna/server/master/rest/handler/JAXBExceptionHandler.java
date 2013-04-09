/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest.handler;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;

@Provider
public class JAXBExceptionHandler extends HandlerCore implements
		ExceptionMapper<JAXBException> {
	@Override
	public Response toResponse(JAXBException exn) {
		return respond(FORBIDDEN, "APIEpicFail: " + exn.getErrorCode(), exn);
	}
}
