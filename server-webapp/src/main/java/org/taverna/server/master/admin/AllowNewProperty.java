package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.BoolProperty;

public class AllowNewProperty implements BoolProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	AllowNewProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public boolean get() {
		return this.adminBean.state.getAllowNewWorkflowRuns();
	}

	@Override
	public boolean set(boolean newValue) {
		this.adminBean.state.setAllowNewWorkflowRuns(newValue);
		return get();
	}
}