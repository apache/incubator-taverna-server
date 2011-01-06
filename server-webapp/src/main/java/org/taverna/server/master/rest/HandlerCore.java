package org.taverna.server.master.rest;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.status;
import static org.taverna.server.master.TavernaServerImpl.log;

import javax.ws.rs.core.Response;

import org.taverna.server.master.ManagementModel;

/**
 * Base class for handlers that grants Spring-enabled access to the management
 * model.
 * 
 * @author Donal Fellows
 */
public class HandlerCore {
	protected ManagementModel managementModel;

	/**
	 * @param managementModel
	 *            the managementModel to set
	 */
	public void setManagementModel(ManagementModel managementModel) {
		this.managementModel = managementModel;
	}

	/**
	 * Simplified interface for building responses.
	 * 
	 * @param status
	 *            What status code to use?
	 * @param exception
	 *            What exception to report on?
	 * @return The build response.
	 */
	protected Response respond(Response.Status status, Exception exception) {
		if (managementModel.getLogOutgoingExceptions())
			log.info("converting exception to response", exception);
		return status(status).type(TEXT_PLAIN_TYPE)
				.entity(exception.getMessage()).build();
	}
}
