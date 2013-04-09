/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

/**
 * Convert a string (URL, etc) to a version that is contextualized to the
 * web-application.
 * 
 * @author Donal Fellows
 */
public class Contextualizer implements ServletContextAware {
	static final String SUBSTITUAND = "%{WEBAPPROOT}";

	/**
	 * Apply the contextualization operation. This consists of replacing the
	 * string <tt>{@value #SUBSTITUAND}</tt> with the real root of the webapp.
	 * 
	 * @param input
	 *            the string to contextualize
	 * @return the contextualized string
	 */
	public String contextualize(String input) {
		// Hack to work around bizarre CXF bug
		return input.replace(SUBSTITUAND,
				context.getRealPath("/").replace("%2D", "-"));
	}

	private ServletContext context;

	@Override
	public void setServletContext(ServletContext servletContext) {
		context = servletContext;
	}
}
