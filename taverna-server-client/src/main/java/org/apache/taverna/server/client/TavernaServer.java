package org.apache.taverna.server.client;
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

import static java.nio.file.Files.readAllBytes;
import static org.apache.taverna.server.client.wadl.TavernaServer.createClient;
import static org.apache.taverna.server.client.wadl.TavernaServer.root;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.server.client.generic.Capability;
import org.apache.taverna.server.client.generic.TavernaRun;
import org.apache.taverna.server.client.generic.VersionedElement;
import org.apache.taverna.server.client.wadl.TavernaServer.Root;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * Client connection to a Taverna Server.
 * <p>
 * To construct a {@link TavernaServer}, use
 * {@link TavernaServerConnectionFactory}.
 * <p>
 * This class provides the general information from the server, e.g.
 * {@link #getCapabilities()} and {@link #getPermittedWorkflows()}.
 * <p>
 * Use {@link #getExistingRuns()} to list existing {@link Run}s, or use
 * {@link #createWorkflowRun(File)} and friends to create a new run.
 *
 * @see TavernaServerConnectionFactory
 */
public class TavernaServer extends Connected {
	final Root root;
	private final URI location;
	private final boolean authenticated;

	TavernaServer(URI serviceRoot) {
		root = root(createClient(), location = serviceRoot);
		authenticated = false;
	}

	TavernaServer(URI serviceRoot, String username, String password) {
		Client client = createClient();
		client.addFilter(new HTTPBasicAuthFilter(username, password));
		authenticated = true;
		root = root(client, location = serviceRoot);
	}

	TavernaServer(TavernaServer service, String username, String password) {
		Client client = createClient();
		client.addFilter(new HTTPBasicAuthFilter(username, password));
		authenticated = true;
		root = root(client, location = service.location);
		getServerVersionInfo();
	}

	public TavernaServer upgradeToAuth(String username, String password) {
		if (authenticated)
			throw new IllegalStateException("may only upgrade an unauthenticated connection");
		return new TavernaServer(this, username, password);
	}

	public List<Capability> getCapabilities() {
		return root.policy().capabilities().getAsCapabilitiesXml().getCapability();
	}

	public int getRunLimit() {
		return root.policy().runLimit().getAsTextPlain(Integer.class);
	}

	public int getOperatingLimit() {
		return root.policy().operatingLimit().getAsTextPlain(Integer.class);
	}

	public List<String> getPermittedWorkflows() {
		return root.policy().permittedWorkflows().getAsPermittedWorkflowsXml().getWorkflow();
	}

	public List<Run> getExistingRuns() {
		List<Run> runs = new ArrayList<>();
		for (TavernaRun run : root.runs().getAsRunListXml().getRun())
			runs.add(new Run(this, run.getValue()));
		return runs;
	}

	public VersionedElement getServerVersionInfo() {
		return root.getAsServerDescriptionXml();
	}

	private Run response2run(ClientResponse response) throws ClientException, ServerException {
		checkError(response);
		if (response.getClientResponseStatus().getStatusCode() == 201) {
			String[] path = response.getLocation().getPath().split("/");
			return new Run(this, path[path.length - 1]);
		}
		return null;
	}

	/**
	 * Create a new Run by uploading the bytes of a t2flow workflow definition.
	 * <p>
	 * The returned {@link Run} be configured (e.g. with 
	 * {@link Run#setInput(String, String)}) before invoking it 
	 * with {@link Run#start()}.
	 * 
	 * @param t2flowBytes Content of workflow definition file to upload, should be in the format <code>application/vnd.taverna.t2flow+xml</code>
	 * @return A {@link Run} that is {@link Status#Initialized}
	 * @throws ClientException If client configuration failed, e.g. AuthorizationException
	 * @throws ServerException If the server refuses upload (e.g. because only {@link #getPermittedWorkflows()} are allowed)
	 */	
	public Run createWorkflowRun(byte[] t2flowBytes) throws ClientException, ServerException {
		return response2run(root.runs().postVndTavernaT2flowXmlAsOctetStream(t2flowBytes, ClientResponse.class));
	}

	/**
	 * Create a new Run by uploading a local t2flow workflow definition File.
	 * <p>
	 * The returned {@link Run} be configured (e.g. with
	 * {@link Run#setInput(String, String)}) before invoking it 
	 * with {@link Run#start()}.
	 * 
	 * @param t2flowFile File of workflow to upload, typically with the extension <code>.t2flow</code>
	 * @return A {@link Run} that is {@link Status#Initialized}
	 * @throws IOException If the file can't be read or a network error occurs
	 * @throws ClientException If client configuration failed, e.g. AuthorizationException
	 * @throws ServerException If the server refuses upload (e.g. because only {@link #getPermittedWorkflows()} are allowed)
	 */
	public Run createWorkflowRun(File t2flowFile) throws IOException, ClientException, ServerException {
		return createWorkflowRun(readAllBytes(t2flowFile.toPath()));
	}

	/**
	 * Create a new Run by referencing an external t2flow workflow definition URI.
	 * <p>
	 * The returned {@link Run} be configured (e.g. with 
	 * {@link Run#setInput(String, String)}) before invoking it 
	 * with {@link Run#start()}.
	 * 
	 * @param t2flowUri URI of workflow to run, should have content-type <code>application/vnd.taverna.t2flow+xml</code>
	 * @return A {@link Run} that is {@link Status#Initialized}
	 * @throws ClientException If client configuration failed, e.g. AuthorizationException
	 * @throws ServerException If the server refuses the URI (e.g. it could not be retrieved)
	 */
	
	public Run createWorkflowRun(URI t2flowUri) throws ClientException, ServerException {
		return response2run(root.runs().postTextUriListAsOctetStream(t2flowUri.toString(), ClientResponse.class));
	}

	public static class ClientException extends Exception {
		private static final long serialVersionUID = 1L;

		ClientException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	public static class AuthorizationException extends ClientException {
		private static final long serialVersionUID = 1L;

		AuthorizationException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	static class ServerException extends Exception {
		private static final long serialVersionUID = 1L;

		ServerException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
