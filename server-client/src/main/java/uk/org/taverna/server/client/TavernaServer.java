package uk.org.taverna.server.client;

import static java.io.File.createTempFile;
import static java.nio.file.Files.readAllBytes;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.tika.mime.MimeTypes.getDefaultMimeTypes;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.taverna.server.client.wadl.TavernaServer.createClient;
import static org.taverna.server.client.wadl.TavernaServer.root;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MimeTypeException;
import org.joda.time.DateTime;
import org.taverna.server.client.wadl.TavernaServer.Root;
import org.taverna.server.client.wadl.TavernaServer.Root.RunsRunName;
import org.w3c.dom.Element;

import uk.org.taverna.ns._2010.port.InputPort;
import uk.org.taverna.ns._2010.port.OutputPort;
import uk.org.taverna.ns._2010.xml.server.Capability;
import uk.org.taverna.ns._2010.xml.server.DirectoryEntry;
import uk.org.taverna.ns._2010.xml.server.DirectoryReference;
import uk.org.taverna.ns._2010.xml.server.FileReference;
import uk.org.taverna.ns._2010.xml.server.KeyPairCredential;
import uk.org.taverna.ns._2010.xml.server.PasswordCredential;
import uk.org.taverna.ns._2010.xml.server.TavernaRun;
import uk.org.taverna.ns._2010.xml.server.VersionedElement;
import uk.org.taverna.ns._2010.xml.server.rest.DirectoryContents;
import uk.org.taverna.ns._2010.xml.server.rest.InputDescription;
import uk.org.taverna.ns._2010.xml.server.rest.InputDescription.Value;
import uk.org.taverna.ns._2010.xml.server.rest.MakeDirectory;
import uk.org.taverna.ns._2010.xml.server.rest.UploadFile;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class TavernaServer {
	private Root root;

	/**
	 * States of a workflow run. They are {@link #Initialized Initialized},
	 * {@link #Operating Operating}, {@link #Stopped Stopped}, and
	 * {@link #Finished Finished}. Conceptually, there is also a
	 * <tt>Destroyed</tt> state, but the workflow run does not exist (and hence
	 * can't have its state queried or set) in that case.
	 * 
	 * @author Donal Fellows
	 */
	public static enum Status {
		/**
		 * The workflow run has been created, but is not yet running. The run
		 * will need to be manually moved to {@link #Operating Operating} when
		 * ready.
		 */
		Initialized,
		/**
		 * The workflow run is going, reading input, generating output, etc.
		 * Will eventually either move automatically to {@link #Finished
		 * Finished} or can be moved manually to {@link #Stopped Stopped} (where
		 * supported).
		 */
		Operating,
		/**
		 * The workflow run is paused, and will need to be moved back to
		 * {@link #Operating Operating} manually.
		 */
		Stopped,
		/**
		 * The workflow run has ceased; data files will continue to exist until
		 * the run is destroyed (which may be manual or automatic).
		 */
		Finished
	}

	public static enum Property {
		STDOUT("stdout"), STDERR("stderr"), EXIT_CODE("exitcode"), READY_TO_NOTIFY(
				"readyToNotify"), EMAIL("notificationAddress"), USAGE(
				"usageRecord");

		private String s;

		private Property(String s) {
			this.s = s;
		}

		@Override
		public String toString() {
			return s;
		}
	}

	public class Run {
		private RunsRunName run;

		Run(String value) {
			run = root.runsRunName(value);
		}

		public String getName() {
			return run.name().getAsTextPlain(String.class);
		}

		public void setName(String name) {
			run.name().putTextPlain(name, String.class);
		}

		public Date getExpiry() {
			return dateTimeParser().parseDateTime(
					run.expiry().getAsTextPlain(String.class)).toDate();
		}

		public void setExpiry(Date expiryTimestamp) {
			run.expiry().putTextPlain(
					dateTime().print(new DateTime(expiryTimestamp)),
					String.class);
		}

		public Date getCreate() {
			String timestamp = run.createTime().getAsTextPlain(String.class);
			if (timestamp == null || timestamp.trim().isEmpty())
				return null;
			return dateTimeParser().parseDateTime(timestamp).toDate();
		}

		public Date getStart() {
			String timestamp = run.startTime().getAsTextPlain(String.class);
			if (timestamp == null || timestamp.trim().isEmpty())
				return null;
			return dateTimeParser().parseDateTime(timestamp).toDate();
		}

		public Date getFinish() {
			String timestamp = run.finishTime().getAsTextPlain(String.class);
			if (timestamp == null || timestamp.trim().isEmpty())
				return null;
			return dateTimeParser().parseDateTime(timestamp).toDate();
		}

		public Status getStatus() {
			return Status.valueOf(run.status().getAsTextPlain(String.class));
		}

		public void setStatus(Status status) {
			run.status().putTextPlain(status, String.class);
		}

		public void start() {
			setStatus(Status.Operating);
		}

		public void kill() {
			setStatus(Status.Finished);
		}

		public boolean isRunning() {
			return getStatus() == Status.Operating;
		}

		public String getStandardOutput() {
			return run.stdout().getAsTextPlain(String.class);
		}

		public String getStandardError() {
			return run.stderr().getAsTextPlain(String.class);
		}

		public String getLog() {
			return run.log().getAsTextPlain(String.class);
		}

		public Integer getExitCode() {
			String code = run.listeners().name("io")
					.propertiesPropertyName("exitCode")
					.getAsTextPlain(String.class);
			if (code == null || code.trim().isEmpty())
				return null;
			return Integer.parseInt(code);
		}

		public String getProperty(Property prop) {
			return run.listeners().name("io")
					.propertiesPropertyName(prop.toString())
					.getAsTextPlain(String.class);
		}

		public void setGenerateRunBundle(boolean generateRunBundle) {
			run.generateProvenance().putTextPlain(generateRunBundle,
					String.class);
		}

		public byte[] getRunBundle() {
			return run.runBundle().getAsVndWf4everRobundleZip(byte[].class);
		}

		public List<InputPort> getInputs() {
			return run.input().expected().getAsInputDescriptionXml().getInput();
		}

		public List<OutputPort> getOutputs() {
			return run.output().getAsOutputDescriptionXml().getOutput();
		}

		public void setInput(String name, String value) {
			Value v = new Value();
			v.setValue(value);
			InputDescription idesc = new InputDescription();
			idesc.setValue(v);
			run.input().inputName(name).putXmlAsInputDescription(idesc);
		}

		public void setInput(String name, String value, char listSeparator) {
			Value v = new Value();
			v.setValue(value);
			InputDescription idesc = new InputDescription();
			idesc.setValue(v);
			idesc.setListDelimiter(new String(new char[] { listSeparator }));
			run.input().inputName(name).putXmlAsInputDescription(idesc);
		}

		public byte[] getWorkflow() {
			return run.workflow().getAsVndTavernaT2flowXml(byte[].class);
		}

		// TODO Consider better ways to do this
		public Element getInteractionFeed() {
			return run.interaction().getAsAtomXml(Element.class);
		}

		public Element getInteractionEntry(String id) {
			return run.interaction().id(id).getAsAtomXml(Element.class);
		}

		public Element getUsageRecord() {
			return run.usage().getAsXml(Element.class);
		}

		public Directory getWorkingDirectory() {
			return new Directory();
		}

		public abstract class DirEntry {
			final String path;

			protected DirEntry(String path) {
				this.path = path.replaceFirst("/+$", "");
			}

			public void delete() {
				run.wd().path2(path).deleteAsXml(ClientResponse.class);
			}

			String path(ClientResponse response) {
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

		public class Directory extends DirEntry {
			Directory() {
				super("");
			}

			Directory(String path) {
				super(path);
			}

			public List<DirEntry> list() {
				List<DirEntry> result = new ArrayList<>();
				for (DirectoryEntry de : run.wd().path3(path)
						.getAsXml(DirectoryContents.class).getDirOrFile())
					if (de instanceof DirectoryReference)
						result.add(new Directory(de.getValue()));
					else if (de instanceof FileReference)
						result.add(new File(de.getValue()));
				return result;
			}

			public File createFile(String name, byte[] content) {
				UploadFile uf = new UploadFile();
				uf.setName(name);
				uf.setValue(content);
				return new File(path(run.wd().path(path)
						.putAsXml(uf, ClientResponse.class)));
			}

			public File createFile(String name, java.io.File content) {
				return new File(path(run
						.wd()
						.path(concat(name))
						.putOctetStreamAsXml(
								entity(content, APPLICATION_OCTET_STREAM_TYPE),
								ClientResponse.class)));
			}

			public File createFile(String name, URI source) {
				return new File(path(run
						.wd()
						.path(concat(name))
						.postTextUriListAsXml(source.toString(),
								ClientResponse.class)));
			}

			public Directory createDirectory(String name) {
				MakeDirectory mkdir = new MakeDirectory();
				mkdir.setName(name);
				return new Directory(path(run.wd().path(path)
						.putAsXml(mkdir, ClientResponse.class)));
			}

			public byte[] getZippedContents() {
				return run.wd().path3(path).getAsZip(byte[].class);
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

		public class File extends DirEntry {
			File(String path) {
				super(path);
			}

			public InputStream getAsStream() {
				return run.wd().path3(path).getAsOctetStream(InputStream.class);
			}

			public byte[] get() {
				return run.wd().path3(path).getAsOctetStream(byte[].class);
			}

			public String get(Charset encoding) {
				return new String(run.wd().path3(path)
						.getAsOctetStream(byte[].class), encoding);
			}

			public java.io.File getAsFile() throws ClientHandlerException,
					UniformInterfaceException, IOException, MimeTypeException {
				ClientResponse cr = run.wd().path3(path)
						.getAsOctetStream(ClientResponse.class);
				String[] bits = localName().split("[.]");
				String ext = getDefaultMimeTypes().forName(
						cr.getHeaders().getFirst("Content-Type"))
						.getExtension();
				if (ext == null)
					ext = bits[bits.length-1];
				java.io.File tmp = createTempFile(bits[0], ext);
				try (OutputStream os = new FileOutputStream(tmp);
						InputStream is = cr.getEntity(InputStream.class)) {
					copy(is, os);
				}
				return tmp;
			}

			public void setContents(byte[] newContents) {
				run.wd().path(path)
						.putOctetStreamAsXml(newContents, ClientResponse.class);
			}

			public void setContents(String newContents) {
				run.wd().path(path)
						.putOctetStreamAsXml(newContents, ClientResponse.class);
			}

			public void setContents(String newContents, Charset encoding) {
				run.wd()
						.path(path)
						.putOctetStreamAsXml(newContents.getBytes(encoding),
								ClientResponse.class);
			}

			public void setContents(InputStream newContents) {
				run.wd().path(path)
						.putOctetStreamAsXml(newContents, ClientResponse.class);
			}

			public void setContents(java.io.File newContents)
					throws IOException {
				run.wd()
						.path(path)
						.putOctetStreamAsXml(
								entity(newContents,
										APPLICATION_OCTET_STREAM_TYPE),
								ClientResponse.class);
			}
		}

		public String getOwner() {
			return run.security().owner().getAsTextPlain(String.class);
		}

		// TODO permissions

		public void grantPasswordCredential(URI contextService,
				String username, String password) {
			PasswordCredential pc = new PasswordCredential();
			pc.setServiceURI(contextService.toString());
			pc.setUsername(username);
			pc.setPassword(password);
			run.security().credentials()
					.postXmlAsOctetStream(pc, ClientResponse.class);
		}

		public void grantKeyCredential(URI contextService, java.io.File source,
				String unlockPassword, String aliasEntry) throws IOException {
			KeyPairCredential kpc = new KeyPairCredential();
			kpc.setServiceURI(contextService.toString());
			try (InputStream in = new FileInputStream(source)) {
				byte[] buffer = new byte[(int) source.length()];
				IOUtils.read(in, buffer);
				kpc.setCredentialBytes(buffer);
			}
			if (source.getName().endsWith(".p12"))
				kpc.setFileType("PKCS12");
			else
				kpc.setFileType("JKS");
			kpc.setCredentialName(aliasEntry);
			kpc.setUnlockPassword(unlockPassword);
			run.security().credentials()
					.postXmlAsOctetStream(kpc, ClientResponse.class);
		}
	}

	public TavernaServer(URI serviceRoot) {
		root = root(createClient(), serviceRoot);
	}

	public TavernaServer(URI serviceRoot, String username, String password) {
		Client client = createClient();
		client.addFilter(new HTTPBasicAuthFilter(username, password));
		root = root(client, serviceRoot);
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
			runs.add(new Run(run.getValue()));
		return runs;
	}

	public VersionedElement getServerVersionInfo() {
		return root.getAsServerDescriptionXml();
	}

	private Run response2run(ClientResponse response) {
		if (response.getClientResponseStatus().getStatusCode() == 201) {
			String[] path = response.getLocation().getPath().split("/");
			return new Run(path[path.length - 1]);
		}
		return null;
	}

	public Run createWorkflowRun(byte[] t2flowBytes) {
		return response2run(root.runs().postVndTavernaT2flowXmlAsOctetStream(
				t2flowBytes, ClientResponse.class));
	}

	public Run createWorkflowRun(File t2flowFile) throws IOException {
		return createWorkflowRun(readAllBytes(t2flowFile.toPath()));
	}

	public Run createWorkflowRun(URI t2flowUri) {
		return response2run(root.runs().postTextUriListAsOctetStream(
				t2flowUri.toString(), ClientResponse.class));
	}
}
