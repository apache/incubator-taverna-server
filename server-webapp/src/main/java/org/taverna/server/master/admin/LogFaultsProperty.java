package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.BoolProperty;

public class LogFaultsProperty implements BoolProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	LogFaultsProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public boolean get() {
		return this.adminBean.state.getLogOutgoingExceptions();
	}

	@Override
	public boolean set(boolean newValue) {
		this.adminBean.state.setLogOutgoingExceptions(newValue);
		return get();
	}
}