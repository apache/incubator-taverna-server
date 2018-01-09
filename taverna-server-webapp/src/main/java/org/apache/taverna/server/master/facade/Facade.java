/*
 */
package org.apache.taverna.server.master.facade;
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
import org.springframework.beans.factory.annotation.Required;
import org.apache.taverna.server.master.utils.Contextualizer;

/**
 * This is a simple class that is used to serve up a file (with a simple
 * substitution applied) as the root of the T2Server webapp.
 * 
 * @author Donal Fellows
 */
@Path("/")
public class Facade {
	private Log log = LogFactory.getLog("Taverna.Server.Utils");
	private String welcome;
	private Contextualizer contextualizer;

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

	@Required
	public void setContextualizer(Contextualizer contextualizer) {
		this.contextualizer = contextualizer;
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
		return ok(contextualizer.contextualize(ui, welcome), TEXT_HTML_TYPE)
				.build();
	}
}
