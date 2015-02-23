/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.UriInfo;

import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.common.version.Version;

/**
 * Convert a string (URL, etc) to a version that is contextualized to the
 * web-application.
 * 
 * @author Donal Fellows
 */
public class Contextualizer implements ServletContextAware {
	static final String ROOT_PLACEHOLDER = "%{WEBAPPROOT}";
	static final String VERSION_PLACEHOLDER = "%{VERSION}";
	static final String BASE_PLACEHOLDER = "%{BASEURL}";

	/**
	 * Apply the contextualization operation. This consists of replacing the
	 * string <tt>{@value #ROOT_PLACEHOLDER}</tt> with the real root of the webapp.
	 * 
	 * @param input
	 *            the string to contextualize
	 * @return the contextualized string
	 */
	public String contextualize(String input) {
		// Hack to work around bizarre CXF bug
		String path = context.getRealPath("/").replace("%2D", "-");
		return input.replace(ROOT_PLACEHOLDER, path).replace(
				VERSION_PLACEHOLDER, Version.JAVA);
	}

	/**
	 * Apply the contextualization operation. This consists of replacing the
	 * string <tt>{@value #ROOT_PLACEHOLDER}</tt> with the real root of the
	 * webapp.
	 * 
	 * @param ui
	 *            Where to get information about the URL used to access the
	 *            webapp.
	 * @param input
	 *            the string to contextualize
	 * @return the contextualized string
	 */
	public String contextualize(UriInfo ui, String input) {
		// Hack to work around bizarre CXF bug
		String baseuri = ui.getBaseUri().toString().replace("%2D", "-");
		if (baseuri.endsWith("/"))
			baseuri = baseuri.substring(0, baseuri.length() - 1);
		return contextualize(input).replace(BASE_PLACEHOLDER, baseuri);
	}

	private ServletContext context;

	@Override
	public void setServletContext(ServletContext servletContext) {
		context = servletContext;
	}
}
