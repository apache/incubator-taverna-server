package org.taverna.server.master.api;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerSupport;

/**
 * Indicates that this is a class that wants to be told by Spring about the
 * main support bean.
 * 
 * @author Donal Fellows
 */
public interface SupportAware {
	/**
	 * How to tell the bean about the support bean.
	 * 
	 * @param support
	 *            Reference to the support bean.
	 */
	@Required
	void setSupport(TavernaServerSupport support);
}