package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class RunAsPasswordFileProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	RunAsPasswordFileProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getPasswordFile();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setPasswordFile(newValue);
		return get();
	}
}