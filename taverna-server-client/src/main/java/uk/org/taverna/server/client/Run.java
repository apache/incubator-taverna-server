package uk.org.taverna.server.client;

import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.ogf.usage.JobUsageRecord;
import org.taverna.server.client.wadl.TavernaServer.Root.RunsRunName;
import org.w3c.dom.Element;

import uk.org.taverna.server.client.TavernaServer.ClientException;
import uk.org.taverna.server.client.TavernaServer.ServerException;
import uk.org.taverna.server.client.generic.KeyPairCredential;
import uk.org.taverna.server.client.generic.PasswordCredential;
import uk.org.taverna.server.client.generic.port.InputPort;
import uk.org.taverna.server.client.generic.port.OutputPort;
import uk.org.taverna.server.client.rest.InputDescription;
import uk.org.taverna.server.client.rest.InputDescription.Value;

import com.sun.jersey.api.client.ClientResponse;

public class Run extends Connected {
	RunsRunName run;

	Run(TavernaServer server, String value) {
		run = server.root.runsRunName(value);
	}

	public String getName() {
		return run.name().getAsTextPlain(ClientResponse.class)
				.getEntity(String.class);
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
				dateTime().print(new DateTime(expiryTimestamp)), String.class);
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
		run.generateProvenance().putTextPlain(generateRunBundle, String.class);
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

	public JobUsageRecord getUsageRecord() throws JAXBException {
		return JobUsageRecord.unmarshal(run.usage().getAsXml(Element.class));
	}

	public Directory getWorkingDirectory() {
		return new Directory(this);
	}

	public String getOwner() {
		return run.security().owner().getAsTextPlain(String.class);
	}

	// TODO permissions

	public void grantPasswordCredential(URI contextService, String username,
			String password) throws ClientException, ServerException {
		PasswordCredential pc = new PasswordCredential();
		pc.setServiceURI(contextService.toString());
		pc.setUsername(username);
		pc.setPassword(password);
		checkError(run.security().credentials()
				.postXmlAsOctetStream(pc, ClientResponse.class));
	}

	public void grantKeyCredential(URI contextService, java.io.File source,
			String unlockPassword, String aliasEntry) throws IOException,
			ClientException, ServerException {
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
		checkError(run.security().credentials()
				.postXmlAsOctetStream(kpc, ClientResponse.class));
	}
}