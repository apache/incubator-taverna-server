/**
 * 
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

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;

/**
 * Logs the time it takes to service HTTP calls into Taverna Server.
 * <p>
 * This class is currently not used.
 * 
 * @author Donal Fellows
 */
public class CallTimingFilter implements Filter {
	private Log log;
	private String name;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log = getLog("Taverna.Server.Performance");
		name = filterConfig.getInitParameter("name");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest)
			doFilter((HttpServletRequest) request,
					(HttpServletResponse) response, chain);
		else
			chain.doFilter(request, response);
	}

	public void doFilter(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		long start = nanoTime();
		chain.doFilter(request, response);
		long elapsedTime = nanoTime() - start;
		log.info(format("%s call to %s %s took %.3fms", name,
				request.getMethod(), request.getRequestURI(),
				elapsedTime / 1000000.0));
	}

	@Override
	public void destroy() {
		log = null;
	}
}
