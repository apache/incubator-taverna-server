/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static java.util.UUID.randomUUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerImpl.WebappAware;
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

/**
 * RESTful interface to the input descriptor of a single workflow run.
 * 
 * @author Donal Fellows
 */
abstract class InputREST implements TavernaServerInputREST, WebappAware {
	private UriInfo ui;
	private TavernaServer webapp;
	private TavernaRun run;
	private ContentsDescriptorBuilder cdBuilder;
	private FilenameUtils fileUtils;

	@Override
	public void setWebapp(TavernaServer webapp) {
		this.webapp = webapp;
	}

	@Required
	public void setCdBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	@Required
	public void setFileUtils(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	void setUriInfo(UriInfo ui) {
		this.ui = ui;
	}

	void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
	public InputsDescriptor get() {
		return new InputsDescriptor(ui, run);
	}

	@Override
	public org.taverna.server.input_description.InputDescription get(String type) {
		if (!type.equals("inputDescription"))
			throw new RuntimeException("boom!");
		return cdBuilder.makeInputDescriptor(run, ui.getAbsolutePathBuilder());
	}

	@Override
	public String getBaclavaFile() {
		String i = run.getInputBaclavaFile();
		return i == null ? "" : i;
	}

	@Override
	public InDesc getInput(String name) throws BadInputPortNameException {
		Input i = TavernaServerImpl.getInput(run, name);
		if (i == null)
			throw new BadInputPortNameException("unknown input port name");
		return new InDesc(i);
	}

	@Override
	public String setBaclavaFile(String filename) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException {
		webapp.permitUpdate(run);
		run.setInputBaclavaFile(filename);
		String i = run.getInputBaclavaFile();
		return i == null ? "" : i;
	}

	@Override
	public InDesc setInput(String name, InDesc inputDescriptor)
			throws NoUpdateException, BadStateChangeException,
			FilesystemAccessException, BadInputPortNameException,
			BadPropertyValueException {
		AbstractContents ac = inputDescriptor.assignment;
		if (name == null || name.isEmpty())
			throw new BadInputPortNameException("bad input name");
		if (ac == null)
			throw new BadPropertyValueException("no content!");
		if (ac instanceof InDesc.Reference)
			return setRemoteInput(name, (InDesc.Reference) ac);
		if (!(ac instanceof InDesc.File || ac instanceof InDesc.Value))
			throw new BadPropertyValueException("unknown content type");
		webapp.permitUpdate(run);
		Input i = TavernaServerImpl.getInput(run, name);
		if (i == null)
			i = run.makeInput(name);
		if (ac instanceof InDesc.File)
			i.setFile(ac.contents);
		else
			i.setValue(ac.contents);
		return new InDesc(i);
	}

	private InDesc setRemoteInput(String name, Reference ref)
			throws BadStateChangeException, BadPropertyValueException,
			FilesystemAccessException {
		URITemplate t = new URITemplate(ui.getBaseUri()
				+ "/runs/{runName}/wd/{path:.+}");
		MultivaluedMap<String, String> mvm = new MetadataMap<String, String>();
		if (!t.match(ref.contents, mvm)) {
			throw new BadPropertyValueException(
					"URI in reference does not refer to local disk resource");
		}
		try {
			File from = fileUtils.getFile(
					webapp.getRun(mvm.get("runName").get(0)),
					FalseDE.make(mvm.get("path").get(0)));
			File to = run.getWorkingDirectory().makeEmptyFile(
					webapp.getPrincipal(), randomUUID().toString());
			to.copy(from);
			Input i = TavernaServerImpl.getInput(run, name);
			if (i == null)
				i = run.makeInput(name);
			i.setFile(to.getFullName());
			return new InDesc(i);
		} catch (UnknownRunException e) {
			throw new BadStateChangeException("may not copy from that run", e);
		} catch (NoDirectoryEntryException e) {
			throw new BadStateChangeException("source does not exist", e);
		}
	}

	static class FalseDE implements DirectoryEntry {
		public static DirEntryReference make(String path) {
			return DirEntryReference.newInstance(new FalseDE(path));
		}

		private FalseDE(String p) {
			this.p = p;
		}

		private String p;

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String getFullName() {
			return p;
		}

		@Override
		public void destroy() throws FilesystemAccessException {
		}
	}
}