package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.BoolProperty;

public class LogWorkflowsProperty implements BoolProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	LogWorkflowsProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public boolean get() {
		return this.adminBean.state.getLogIncomingWorkflows();
	}

	@Override
	public boolean set(boolean newValue) {
		this.adminBean.state.setLogIncomingWorkflows(newValue);
		return get();
	}
}