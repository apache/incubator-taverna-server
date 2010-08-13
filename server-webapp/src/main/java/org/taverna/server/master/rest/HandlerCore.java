package org.taverna.server.master.rest;

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
}
