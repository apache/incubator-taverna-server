/**
 * 
 */
package org.taverna.server.master.utils;

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
