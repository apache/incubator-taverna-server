package uk.org.taverna.server.client;

import static java.io.File.createTempFile;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.taverna.server.client.wadl.TavernaServer.Root.RunsRunName.Wd;

import uk.org.taverna.server.client.TavernaServer.ClientException;
import uk.org.taverna.server.client.TavernaServer.ServerException;
import uk.org.taverna.server.client.generic.DirectoryEntry;
import uk.org.taverna.server.client.generic.DirectoryReference;
import uk.org.taverna.server.client.generic.FileReference;
import uk.org.taverna.server.client.rest.DirectoryContents;
import uk.org.taverna.server.client.rest.MakeDirectory;
import uk.org.taverna.server.client.rest.UploadFile;

import com.sun.jersey.api.client.ClientResponse;

public class Directory extends DirEntry {
	private final Wd wd;

	Directory(Run run) {
		super(run, "");
		this.wd = run.run.wd();
	}

	Directory(Run run, String path) {
		super(run, path);
		this.wd = run.run.wd();
	}

	public List<DirEntry> list() {
		List<DirEntry> result = new ArrayList<>();
		for (DirectoryEntry de : wd.path3(path)
				.getAsXml(DirectoryContents.class).getDirOrFile())
			if (de instanceof DirectoryReference)
				result.add(new Directory(run, de.getValue()));
			else if (de instanceof FileReference)
				result.add(new File(run, de.getValue()));
		return result;
	}

	public File createFile(String name, byte[] content) throws ClientException,
			ServerException {
		UploadFile uf = new UploadFile();
		uf.setName(name);
		uf.setValue(content);
		return new File(run, path(wd.path(path).putAsXml(uf,
				ClientResponse.class)));
	}

	public File createFile(String name, java.io.File content)
			throws ClientException, ServerException {
		return new File(run, path(wd.path(concat(name)).putOctetStreamAsXml(
				entity(content, APPLICATION_OCTET_STREAM_TYPE),
				ClientResponse.class)));
	}

	public File createFile(String name, URI source) throws ClientException,
			ServerException {
		return new File(run, path(wd.path(concat(name)).postTextUriListAsXml(
				source.toString(), ClientResponse.class)));
	}

	public Directory createDirectory(String name) throws ClientException,
			ServerException {
		MakeDirectory mkdir = new MakeDirectory();
		mkdir.setName(name);
		return new Directory(run, path(wd.path(path).putAsXml(mkdir,
				ClientResponse.class)));
	}

	public byte[] getZippedContents() {
		return wd.path3(path).getAsZip(byte[].class);
	}

	public ZipFile getZip() throws IOException {
		byte[] contents = getZippedContents();
		java.io.File tmp = createTempFile(localName(), ".zip");
		try (OutputStream os = new FileOutputStream(tmp)) {
			os.write(contents);
		}
		return new ZipFile(tmp);
	}
}