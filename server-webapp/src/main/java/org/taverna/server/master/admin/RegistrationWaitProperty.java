package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.IntegerProperty;

public class RegistrationWaitProperty implements IntegerProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RegistrationWaitProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public int get() {
		return this.adminBean.factory.getWaitSeconds();
	}

	@Override
	public int set(int newValue) {
		this.adminBean.factory.setWaitSeconds(newValue);
		return get();
	}
}