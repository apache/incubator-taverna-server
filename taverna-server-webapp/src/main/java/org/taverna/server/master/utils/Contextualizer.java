/*
 */
package org.taverna.server.master.utils;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
