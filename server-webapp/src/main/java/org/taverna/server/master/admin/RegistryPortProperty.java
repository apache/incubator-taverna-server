package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.IntegerProperty;

public class RegistryPortProperty implements IntegerProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RegistryPortProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public int get() {
		return this.adminBean.factory.getRegistryPort();
	}

	@Override
	public int set(int newValue) {
		this.adminBean.factory.setRegistryPort(newValue);
		return get();
	}
}