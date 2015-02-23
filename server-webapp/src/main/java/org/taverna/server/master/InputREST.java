/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static java.util.UUID.randomUUID;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.util.Date;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.api.InputBean;
import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.exceptions.BadInputPortNameException;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.rest.TavernaServerInputREST.InDesc.AbstractContents;
import org.taverna.server.master.rest.TavernaServerInputREST.InDesc.Reference;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;
import org.taverna.server.port_description.InputDescription;

/**
 * RESTful interface to the input descriptor of a single workflow run.
 * 
 * @author Donal Fellows
 */
class InputREST implements TavernaServerInputREST, InputBean {
	private UriInfo ui;
	private TavernaServerSupport support;
	private TavernaRun run;
	private ContentsDescriptorBuilder cdBuilder;
	private FilenameUtils fileUtils;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	@Required
	public void setCdBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	@Override
	@Required
	public void setFileUtils(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	@Override
	public InputREST connect(TavernaRun run, UriInfo ui) {
		this.run = run;
		this.ui = ui;
		return this;
	}

	@Override
	@CallCounted
	@PerfLogged
	public InputsDescriptor get() {
		return new InputsDescriptor(ui, run);
	}

	@Override
	@CallCounted
	@PerfLogged
	public InputDescription getExpected() {
		return cdBuilder.makeInputDescriptor(run, ui);
	}

	@Override
	@CallCounted
	@PerfLogged
	public String getBaclavaFile() {
		String i = run.getInputBaclavaFile();
		return i == null ? "" : i;
	}

	@Override
	@CallCounted
	@PerfLogged
	public InDesc getInput(String name, UriInfo ui) throws BadInputPortNameException {
		Input i = support.getInput(run, name);
		if (i == null)
			throw new BadInputPortNameException("unknown input port name");
		return new InDesc(i, ui);
	}

	@Override
	@CallCounted
	@PerfLogged
	public String setBaclavaFile(String filename) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException {
		support.permitUpdate(run);
		run.setInputBaclavaFile(filename);
		String i = run.getInputBaclavaFile();
		return i == null ? "" : i;
	}

	@Override
	@CallCounted
	@PerfLogged
	public InDesc setInput(String name, InDesc inputDescriptor, UriInfo ui)
			throws NoUpdateException, BadStateChangeException,
			FilesystemAccessException, BadInputPortNameException,
			BadPropertyValueException {
		inputDescriptor.descriptorRef = null;
		AbstractContents ac = inputDescriptor.assignment;
		if (name == null || name.isEmpty())
			throw new BadInputPortNameException("bad input name");
		if (ac == null)
			throw new BadPropertyValueException("no content!");
		if (inputDescriptor.delimiter != null
				&& inputDescriptor.delimiter.isEmpty())
			inputDescriptor.delimiter = null;
		if (ac instanceof InDesc.Reference)
			return setRemoteInput(name, (InDesc.Reference) ac,
					inputDescriptor.delimiter, ui);
		if (!(ac instanceof InDesc.File || ac instanceof InDesc.Value))
			throw new BadPropertyValueException("unknown content type");
		support.permitUpdate(run);
		Input i = support.getInput(run, name);
		if (i == null)
			i = run.makeInput(name);
		if (ac instanceof InDesc.File)
			i.setFile(ac.contents);
		else
			i.setValue(ac.contents);
		i.setDelimiter(inputDescriptor.delimiter);
		return new InDesc(i, ui);
	}

	private InDesc setRemoteInput(String name, Reference ref, String delimiter,
			UriInfo ui) throws BadStateChangeException,
			BadPropertyValueException, FilesystemAccessException {
		URITemplate tmpl = new URITemplate(ui.getBaseUri()
				+ "/runs/{runName}/wd/{path:.+}");
		MultivaluedMap<String, String> mvm = new MetadataMap<>();
		if (!tmpl.match(ref.contents, mvm)) {
			throw new BadPropertyValueException(
					"URI in reference does not refer to local disk resource");
		}
		try {
			File from = fileUtils.getFile(
					support.getRun(mvm.get("runName").get(0)),
					SyntheticDirectoryEntry.make(mvm.get("path").get(0)));
			File to = run.getWorkingDirectory().makeEmptyFile(
					support.getPrincipal(), randomUUID().toString());

			to.copy(from);

			Input i = support.getInput(run, name);
			if (i == null)
				i = run.makeInput(name);
			i.setFile(to.getFullName());
			i.setDelimiter(delimiter);
			return new InDesc(i, ui);
		} catch (UnknownRunException e) {
			throw new BadStateChangeException("may not copy from that run", e);
		} catch (NoDirectoryEntryException e) {
			throw new BadStateChangeException("source does not exist", e);
		}
	}

	@Override
	@CallCounted
	public Response options() {
		return opt();
	}

	@Override
	@CallCounted
	public Response expectedOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response baclavaOptions() {
		return opt("PUT");
	}

	@Override
	@CallCounted
	public Response inputOptions(@PathParam("name") String name) {
		return opt("PUT");
	}
}

/**
 * A way to create synthetic directory entries, used during deletion.
 * 
 * @author Donal Fellows
 */
class SyntheticDirectoryEntry implements DirectoryEntry {
	public static DirEntryReference make(String path) {
		return DirEntryReference.newInstance(new SyntheticDirectoryEntry(path));
	}

	private SyntheticDirectoryEntry(String p) {
		this.p = p;
		this.d = new Date();
	}

	private String p;
	private Date d;

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String getFullName() {
		return p;
	}

	@Override
	public void destroy() {
	}

	@Override
	public int compareTo(DirectoryEntry o) {
		return p.compareTo(o.getFullName());
	}

	@Override
	public Date getModificationDate() {
		return d;
	}
}
