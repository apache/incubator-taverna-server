package org.taverna.server.master.admin;

import java.util.Arrays;

import org.taverna.server.master.admin.Admin.StringList;
import org.taverna.server.master.admin.Admin.StringListProperty;

public class ExtraArgsProperty implements StringListProperty {
	/**
	 * 
	 */
	private final AdminBean adminBean;

	/**
	 * @param adminBean
	 */
	ExtraArgsProperty(AdminBean adminBean) {
		this.adminBean = adminBean;
	}

	@Override
	public StringList get() {
		StringList result = new StringList();
		result.string = Arrays.asList(this.adminBean.factory.getExtraArguments());
		return result;
	}

	@Override
	public StringList set(StringList newValue) {
		this.adminBean.factory.setExtraArguments(newValue.string
				.toArray(new String[newValue.string.size()]));
		return get();
	}
}