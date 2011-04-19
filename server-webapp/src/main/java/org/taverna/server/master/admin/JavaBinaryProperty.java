package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class JavaBinaryProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	JavaBinaryProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.factory.getJavaBinary();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.factory.setJavaBinary(newValue);
		return get();
	}
}