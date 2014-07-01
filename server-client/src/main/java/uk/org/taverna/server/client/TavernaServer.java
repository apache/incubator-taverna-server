package uk.org.taverna.server.client;

import static java.nio.file.Files.readAllBytes;
import static org.taverna.server.client.wadl.TavernaServer.createClient;
import static org.taverna.server.client.wadl.TavernaServer.root;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.taverna.server.client.wadl.TavernaServer.Root;

import uk.org.taverna.server.client.generic.Capability;
import uk.org.taverna.server.client.generic.TavernaRun;
import uk.org.taverna.server.client.generic.VersionedElement;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

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
		return root.policy().capabilities().getAsCapabilitiesXml()
				.getCapability();
	}

	public int getRunLimit() {
		return root.policy().runLimit().getAsTextPlain(Integer.class);
	}

	public int getOperatingLimit() {
		return root.policy().operatingLimit().getAsTextPlain(Integer.class);
	}

	public List<String> getPermittedWorkflows() {
		return root.policy().permittedWorkflows().getAsPermittedWorkflowsXml()
				.getWorkflow();
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

	public Run createWorkflowRun(byte[] t2flowBytes) throws ClientException, ServerException {
		return response2run(root.runs().postVndTavernaT2flowXmlAsOctetStream(
				t2flowBytes, ClientResponse.class));
	}

	public Run createWorkflowRun(File t2flowFile) throws IOException, ClientException, ServerException {
		return createWorkflowRun(readAllBytes(t2flowFile.toPath()));
	}

	public Run createWorkflowRun(URI t2flowUri) throws ClientException, ServerException {
		return response2run(root.runs().postTextUriListAsOctetStream(
				t2flowUri.toString(), ClientResponse.class));
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
