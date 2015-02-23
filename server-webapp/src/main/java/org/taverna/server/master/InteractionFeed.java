/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static org.taverna.server.master.common.Roles.SELF;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.taverna.server.master.api.FeedBean;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interaction.InteractionFeedSupport;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.InteractionFeedREST;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * How to connect an interaction feed to the webapp.
 * 
 * @author Donal Fellows
 */
public class InteractionFeed implements InteractionFeedREST, FeedBean {
	private InteractionFeedSupport interactionFeed;
	private TavernaRun run;

	@Override
	public void setInteractionFeedSupport(InteractionFeedSupport feed) {
		this.interactionFeed = feed;
	}

	InteractionFeed connect(TavernaRun run) {
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public Feed getFeed() throws FilesystemAccessException,
			NoDirectoryEntryException {
		return interactionFeed.getRunFeed(run);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public Response addEntry(Entry entry) throws MalformedURLException,
			FilesystemAccessException, NoDirectoryEntryException,
			NoUpdateException {
		Entry realEntry = interactionFeed.addRunFeedEntry(run, entry);
		URI location;
		try {
			location = realEntry.getSelfLink().getHref().toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException("failed to make URI from link?!", e);
		}
		return Response.created(location).entity(realEntry)
				.type("application/atom+xml;type=entry").build();
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public Entry getEntry(String id) throws FilesystemAccessException,
			NoDirectoryEntryException {
		return interactionFeed.getRunFeedEntry(run, id);
	}

	@Override
	@CallCounted
	@PerfLogged
	@RolesAllowed({ USER, SELF })
	public String deleteEntry(String id) throws FilesystemAccessException,
			NoDirectoryEntryException, NoUpdateException {
		interactionFeed.removeRunFeedEntry(run, id);
		return "entry successfully deleted";
	}

	@Override
	@CallCounted
	public Response feedOptions() {
		return opt("POST");
	}

	@Override
	@CallCounted
	public Response entryOptions(String id) {
		return opt("DELETE");
	}
}