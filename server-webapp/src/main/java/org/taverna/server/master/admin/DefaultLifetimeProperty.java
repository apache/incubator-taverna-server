package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.IntegerProperty;

public class DefaultLifetimeProperty implements IntegerProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	DefaultLifetimeProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public int get() {
		return this.adminBean.factory.getDefaultLifetime();
	}

	@Override
	public int set(int newValue) {
		this.adminBean.factory.setDefaultLifetime(newValue);
		return get();
	}
}