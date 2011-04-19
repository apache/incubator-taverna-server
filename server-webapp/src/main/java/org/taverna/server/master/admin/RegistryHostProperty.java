package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class RegistryHostProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RegistryHostProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getRegistryHost();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setRegistryHost(newValue);
		return get();
	}
}