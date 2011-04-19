package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.IntegerProperty;

public class RunLimitProperty implements IntegerProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RunLimitProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public int get() {
		return this.adminBean.factory.getMaxRuns();
	}

	@Override
	public int set(int newValue) {
		this.adminBean.factory.setMaxRuns(newValue);
		return get();
	}
}