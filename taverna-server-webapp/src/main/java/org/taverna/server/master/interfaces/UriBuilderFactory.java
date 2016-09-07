/*
 */
package org.taverna.server.master.interfaces;
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

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

/**
 * How to manufacture URIs to workflow runs.
 * 
 * @author Donal Fellows
 */
public interface UriBuilderFactory {
	/**
	 * Given a run, get a factory for RESTful URIs to resources associated
	 * with it.
	 * 
	 * @param run
	 *            The run in question.
	 * @return The {@link URI} factory.
	 */
	UriBuilder getRunUriBuilder(TavernaRun run);

	/**
	 * @return a URI factory that is preconfigured to point to the base of
	 *         the webapp.
	 */
	UriBuilder getBaseUriBuilder();

	/**
	 * Resolves a URI with respect to the base URI of the factory.
	 * 
	 * @param uri
	 *            The URI to resolve, or <tt>null</tt>.
	 * @return The resolved URI, or <tt>null</tt> if <b>uri</b> is
	 *         <tt>null</tt>.
	 */
	String resolve(String uri);
}