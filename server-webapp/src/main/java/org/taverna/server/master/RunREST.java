/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.taverna.server.master.TavernaServer.log;
import static org.taverna.server.master.common.Roles.SELF;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.common.Status.Initialized;
import static org.taverna.server.master.common.Status.Operating;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import org.joda.time.DateTime;
import org.ogf.usage.JobUsageRecord;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.api.RunBean;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.NotOwnerException;
import org.taverna.server.master.exceptions.OverloadedException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.InteractionFeedREST;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.rest.TavernaServerRunREST;
import org.taverna.server.master.rest.TavernaServerSecurityREST;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;
import org.taverna.server.port_description.OutputDescription;

/**
 * RESTful interface to a single workflow run.
 * 
 * @author Donal Fellows
 */
abstract class RunREST implements TavernaServerRunREST, RunBean {
	private String runName;
	private TavernaRun run;
	private TavernaServerSupport support;
	private ContentsDescriptorBuilder cdBuilder;

	@Override
	@Required
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	@Required
	public void setCdBuilder(ContentsDescriptorBuilder cdBuilder) {
		this.cdBuilder = cdBuilder;
	}

	@Override
	public void setRunName(String runName) {
		this.runName = runName;
	}

	@Override
	public void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
	@CallCounted
	@PerfLogged
	public RunDescription getDescription(UriInfo ui) {
		return new RunDescription(run, ui);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Response destroy() throws NoUpdateException {
		try {
			support.unregisterRun(runName, run);
		} catch (UnknownRunException e) {
			log.fatal("can't happen", e);
		}
		return noContent().build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public TavernaServerListenersREST getListeners() {
		return makeListenersInterface().connect(run);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public TavernaServerSecurityREST getSecurity() throws NotOwnerException {
		TavernaSecurityContext secContext = run.getSecurityContext();
		if (!support.getPrincipal().equals(secContext.getOwner()))
			throw new NotOwnerException();

		// context.getBean("run.security", run, secContext);
		return makeSecurityInterface().connect(secContext, run);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getExpiryTime() {
		return dateTime().print(new DateTime(run.getExpiry()));
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getCreateTime() {
		return dateTime().print(new DateTime(run.getCreationTimestamp()));
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getFinishTime() {
		Date f = run.getFinishTimestamp();
		return f == null ? "" : dateTime().print(new DateTime(f));
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getStartTime() {
		Date f = run.getStartTimestamp();
		return f == null ? "" : dateTime().print(new DateTime(f));
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getStatus() {
		return run.getStatus().toString();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Workflow getWorkflow() {
		return run.getWorkflow();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public DirectoryREST getWorkingDirectory() {
		return makeDirectoryInterface().connect(run);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String setExpiryTime(String expiry) throws NoUpdateException,
			IllegalArgumentException {
		DateTime wanted = dateTimeParser().parseDateTime(expiry.trim());
		Date achieved = support.updateExpiry(run, wanted.toDate());
		return dateTime().print(new DateTime(achieved));
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Response setStatus(String status) throws NoUpdateException {
		Status newStatus = Status.valueOf(status.trim());
		support.permitUpdate(run);
		if (newStatus == Operating && run.getStatus() == Initialized) {
			if (!support.getAllowStartWorkflowRuns())
				throw new OverloadedException();
			String issue = run.setStatus(newStatus);
			if (issue == null)
				issue = "starting run...";
			return status(202).entity(issue).type("text/plain").build();
		}
		run.setStatus(newStatus); // Ignore the result
		return ok(run.getStatus().toString()).type("text/plain").build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public TavernaServerInputREST getInputs(UriInfo ui) {
		return makeInputInterface().connect(run, ui);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getOutputFile() {
		String o = run.getOutputBaclavaFile();
		return o == null ? "" : o;
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
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
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public OutputDescription getOutputDescription(UriInfo ui)
			throws BadStateChangeException, FilesystemAccessException,
			NoDirectoryEntryException {
		if (run.getStatus() == Initialized)
			throw new BadStateChangeException(
					"may not get output description in initial state");
		return cdBuilder.makeOutputDescriptor(run, ui);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public InteractionFeedREST getInteractionFeed() {
		return makeInteractionFeed().connect(run);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getName() {
		return run.getName();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String setName(String name) throws NoUpdateException {
		support.permitUpdate(run);
		run.setName(name);
		return run.getName();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getStdout() throws NoListenerException {
		return support.getProperty(run, "io", "stdout");
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public String getStderr() throws NoListenerException {
		return support.getProperty(run, "io", "stderr");
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Response getUsage() throws NoListenerException, JAXBException {
		String ur = support.getProperty(run, "io", "usageRecord");
		if (ur.isEmpty())
			return noContent().build();
		return ok(JobUsageRecord.unmarshal(ur), APPLICATION_XML).build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Response getLogContents() {
		FileConcatenation fc = support.getLogs(run);
		if (fc.isEmpty())
			return Response.noContent().build();
		return Response.ok(fc, TEXT_PLAIN).build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public Response getRunBundle() {
		FileConcatenation fc = support.getProv(run);
		if (fc.isEmpty())
			return Response.status(404).entity("no provenance currently available").build();
		return Response.ok(fc, "application/vnd.wf4ever.robundle+zip").build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public boolean getGenerateProvenance() {
		return run.getGenerateProvenance();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed(USER)
	public boolean setGenerateProvenance(boolean newValue) throws NoUpdateException {
		support.permitUpdate(run);
		run.setGenerateProvenance(newValue);
		return run.getGenerateProvenance();
	}

	/**
	 * Construct a RESTful interface to a run's filestore.
	 * 
	 * @return The handle to the interface, as decorated by Spring.
	 */
	protected abstract DirectoryREST makeDirectoryInterface();

	/**
	 * Construct a RESTful interface to a run's input descriptors.
	 * 
	 * @return The handle to the interface, as decorated by Spring.
	 */
	protected abstract InputREST makeInputInterface();

	/**
	 * Construct a RESTful interface to a run's listeners.
	 * 
	 * @return The handle to the interface, as decorated by Spring.
	 */
	protected abstract ListenersREST makeListenersInterface();

	/**
	 * Construct a RESTful interface to a run's security.
	 * 
	 * @return The handle to the interface, as decorated by Spring.
	 */
	protected abstract RunSecurityREST makeSecurityInterface();

	/**
	 * Construct a RESTful interface to a run's interaction feed.
	 * 
	 * @return The handle to the interaface, as decorated by Spring.
	 */
	protected abstract InteractionFeed makeInteractionFeed();

	@Override
	@CallCounted
	public Response runOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response workflowOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response expiryOptions() {
		return opt("PUT");
	}

	@Override
	@CallCounted
	public Response createTimeOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response startTimeOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response finishTimeOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response statusOptions() {
		return opt("PUT");
	}

	@Override
	@CallCounted
	public Response outputOptions() {
		return opt("PUT");
	}

	@Override
	@CallCounted
	public Response nameOptions() {
		return opt("PUT");
	}

	@Override
	@CallCounted
	public Response stdoutOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response stderrOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response usageOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response logOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response runBundleOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response generateProvenanceOptions() {
		return opt("PUT");
	}
}
