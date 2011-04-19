package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.IntegerProperty;

public class RegistrationPollProperty implements IntegerProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RegistrationPollProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public int get() {
		return this.adminBean.factory.getSleepTime();
	}

	@Override
	public int set(int newValue) {
		this.adminBean.factory.setSleepTime(newValue);
		return get();
	}
}