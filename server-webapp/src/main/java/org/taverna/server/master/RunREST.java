/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static javax.ws.rs.core.Response.noContent;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.taverna.server.master.TavernaServerImpl.log;
import static org.taverna.server.master.common.Status.Initialized;

import java.util.Date;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerImpl.SupportAware;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.NotOwnerException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.rest.TavernaServerSecurityREST;
import org.taverna.server.output_description.RdfWrapper;

/**
 * RESTful interface to a single workflow run.
 * 
 * @author Donal Fellows
 */
abstract class RunREST implements TavernaServerRunREST, SupportAware {
	private String runName;
	private TavernaRun run;
	private TavernaServerSupport support;
	private ContentsDescriptorBuilder cdBuilder;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Required
	public void setCdBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	void setRunName(String runName) {
		this.runName = runName;
	}

	void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
	public RunDescription getDescription(UriInfo ui) {
		return new RunDescription(run, ui);
	}

	@Override
	public Response destroy() throws NoUpdateException {
		try {
			support.unregisterRun(runName, run);
		} catch (UnknownRunException e) {
			log.fatal("can't happen", e);
		}
		return noContent().build();
	}

	@Override
	public TavernaServerListenersREST getListeners() {
		ListenersREST listeners = makeListenersInterface();
		listeners.setRun(run);
		return listeners;
	}

	@Override
	public TavernaServerSecurityREST getSecurity() throws NotOwnerException {
		TavernaSecurityContext secContext = run.getSecurityContext();
		if (!support.getPrincipal().equals(secContext.getOwner()))
			throw new NotOwnerException();

		// context.getBean("run.security", run, secContext);
		RunSecurityREST sec = makeSecurityInterface();
		sec.setRun(run);
		sec.setSecurityContext(secContext);
		return sec;
	}

	@Override
	public String getExpiryTime() {
		return dateTime().print(new DateTime(run.getExpiry()));
	}

	@Override
	public String getCreateTime() {
		return dateTime().print(new DateTime(run.getCreationTimestamp()));
	}

	@Override
	public String getFinishTime() {
		Date f = run.getFinishTimestamp();
		return f == null ? "" : dateTime().print(new DateTime(f));
	}

	@Override
	public String getStartTime() {
		Date f = run.getStartTimestamp();
		return f == null ? "" : dateTime().print(new DateTime(f));
	}

	@Override
	public String getStatus() {
		return run.getStatus().toString();
	}

	@Override
	public Workflow getWorkflow() {
		return run.getWorkflow();
	}

	@Override
	public DirectoryREST getWorkingDirectory() {
		DirectoryREST d = makeDirectoryInterface();
		d.setRun(run);
		return d;
	}

	@Override
	public String setExpiryTime(String expiry) throws NoUpdateException,
			IllegalArgumentException {
		DateTime wanted = dateTimeParser().parseDateTime(expiry.trim());
		Date achieved = support.updateExpiry(run, wanted.toDate());
		return dateTime().print(new DateTime(achieved));
	}

	@Override
	public String setStatus(String status) throws NoUpdateException {
		support.permitUpdate(run);
		run.setStatus(Status.valueOf(status.trim()));
		return run.getStatus().toString();
	}

	@Override
	public TavernaServerInputREST getInputs(final UriInfo ui) {
		InputREST input = makeInputInterface();
		input.setRun(run);
		input.setUriInfo(ui);
		return input;
	}

	@Override
	public String getOutputFile() {
		String o = run.getOutputBaclavaFile();
		return o == null ? "" : o;
	}

	@Override
	public String setOutputFile(String filename) throws NoUpdateException,
			FilesystemAccessException, BadStateChangeException {
		support.permitUpdate(run);
		if (filename != null && filename.length() == 0)
			filename = null;
		run.setOutputBaclavaFile(filename);
		String o = run.getOutputBaclavaFile();
		return o == null ? "" : o;
	}

	@Override
	public RdfWrapper getOutputDescription(UriInfo ui)
			throws BadStateChangeException, FilesystemAccessException,
			NoDirectoryEntryException {
		if (run.getStatus() == Initialized)
			throw new BadStateChangeException(
					"may not get output description in initial state");
		return cdBuilder.makeOutputDescriptor(run, ui);
	}

	/** Construct a RESTful interface to a run's filestore. */
	protected abstract DirectoryREST makeDirectoryInterface();

	/** Construct a RESTful interface to a run's input descriptors. */
	protected abstract InputREST makeInputInterface();

	/** Construct a RESTful interface to a run's listeners. */
	protected abstract ListenersREST makeListenersInterface();

	/** Construct a RESTful interface to a run's security. */
	protected abstract RunSecurityREST makeSecurityInterface();
}