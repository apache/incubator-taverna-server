package org.taverna.server.master.admin;

import org.taverna.server.master.admin.Admin.StringProperty;

public class URFileProperty implements StringProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	URFileProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public String get() {
		return this.adminBean.state.getUsageRecordLogFile();
	}

	@Override
	public String set(String newValue) {
		this.adminBean.state.setUsageRecordLogFile(newValue);
		return get();
	}
}