/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.facade;

import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.ok;

import java.io.IOException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a simple class that is used to serve up a file (with a simple
 * substitution applied) as the root of the T2Server webapp.
 * 
 * @author Donal Fellows
 */
@Path("/")
public class Facade {
	private Log log = LogFactory.getLog("Taverna.Server.Utils");
	private String pattern;
	private String welcome;

	/**
	 * Set what to replace with the real service URI.
	 * 
	 * @param pattern
	 *            The regexp pattern that matches the content of the file that
	 *            will be replaced by the actual service address.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Set what resource file to use as the template for the response.
	 * 
	 * @param file
	 *            The file from which to load the data (presumed HTML) to serve
	 *            up as the root content.
	 * @throws IOException
	 *             If the file doesn't exist.
	 */
	public void setFile(String file) throws IOException {
		URL full = Facade.class.getResource(file);
		log.info("setting " + full + " as source of root page");
		this.welcome = IOUtils.toString(full);
	}

	/**
	 * Serve up some HTML as the root of the service.
	 * 
	 * @param ui
	 *            A reference to how we were accessed by the service.
	 * @return The response, containing the HTML.
	 */
	@GET
	@Path("{dummy:.*}")
	@Produces("text/html")
	public Response get(@Context UriInfo ui) {
		String url = ui.getBaseUri().toString().replace("%2D", "-");
		if (!url.endsWith("/"))
			url += "/";
		return ok(welcome.replaceAll(pattern, url), TEXT_HTML_TYPE).build();
	}
}
