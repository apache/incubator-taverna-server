package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class ServerWorkerProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	ServerWorkerProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getServerWorkerJar();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setServerWorkerJar(newValue);
		return get();
	}
}