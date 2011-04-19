package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class ServerForkerProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	ServerForkerProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getServerForkerJar();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setServerForkerJar(newValue);
		return get();
	}
}