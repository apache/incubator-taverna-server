package uk.org.taverna.server.client;

import org.taverna.server.client.wadl.TavernaServer.Root.RunsRunName.Wd.Path2;

import uk.org.taverna.server.client.TavernaServer.ClientException;
import uk.org.taverna.server.client.TavernaServer.ServerException;

import com.sun.jersey.api.client.ClientResponse;

public abstract class DirEntry extends Connected {
	final Path2 handle;
	final String path;
	final Run run;

	protected DirEntry(Run run, String path) {
		this.run = run;
		this.path = path.replaceFirst("/+$", "");
		this.handle = run.run.wd().path2(this.path);
	}

	public void delete() throws ClientException, ServerException {
		checkError(handle.deleteAsXml(ClientResponse.class));
	}

	String path(ClientResponse response) throws ClientException, ServerException {
		checkError(response);
		String[] bits = response.getLocation().getPath().split("/");
		return concat(bits[bits.length - 1]);
	}

	String localName() {
		String[] bits = path.split("/");
		return bits[bits.length - 1];
	}

	String concat(String name) {
		return path + "/" + name.split("/", 2)[0];
	}
}