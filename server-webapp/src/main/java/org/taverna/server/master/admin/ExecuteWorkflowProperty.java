package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class ExecuteWorkflowProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	ExecuteWorkflowProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getExecuteWorkflowScript();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setExecuteWorkflowScript(newValue);
		return get();
	}
}