/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification.atom;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.common.Uri.secure;

import java.net.URI;
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.TavernaServerSupport;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.UriBuilderFactory;
import org.taverna.server.master.rest.TavernaServerREST.EventFeed;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * Simple REST handler that allows an Atom feed to be served up of events
 * generated by workflow runs.
 * 
 * @author Donal Fellows
 */
public class AtomFeed implements EventFeed, UriBuilderFactory,
		ServletContextAware {
	/**
	 * The name of a parameter that states what address we should claim that the
	 * feed's internally-generated URIs are relative to. If not set, a default
	 * will be guessed.
	 */
	public static final String PREFERRED_URI_PARAM = "taverna.preferredUserUri";
	private EventDAO eventSource;
	private TavernaServerSupport support;
	private URI baseURI;
	private Abdera abdera;
	private String feedLanguage = "en";
	private String uuid = randomUUID().toString();

	@Required
	public void setEventSource(EventDAO eventSource) {
		this.eventSource = eventSource;
	}

	@Required
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	public void setFeedLanguage(String language) {
		this.feedLanguage = language;
	}

	public String getFeedLanguage() {
		return feedLanguage;
	}

	@Required
	public void setAbdera(Abdera abdera) {
		this.abdera = abdera;
	}

	@Override
	@CallCounted
	@RolesAllowed(USER)
	public Feed getFeed(UriInfo ui) {
		Feed feed = abdera.getFactory().newFeed();
		feed.setTitle("events relating to workflow runs").setLanguage(
				feedLanguage);
		String user = support.getPrincipal().toString()
				.replaceAll("[^A-Za-z0-9]+", "");
		feed.setId(format("urn:taverna-server:%s:%s", uuid, user));
		org.joda.time.DateTime modification = null;
		for (Event e : eventSource.getEvents(support.getPrincipal())) {
			if (modification == null || e.getPublished().isAfter(modification))
				modification = e.getPublished();
			feed.addEntry(e.getEntry(abdera, feedLanguage));
		}
		if (modification == null)
			feed.setUpdated(new Date());
		else
			feed.setUpdated(modification.toDate());
		feed.addLink(ui.getAbsolutePath().toASCIIString(), "self");
		return feed;
	}

	@Override
	@CallCounted
	@RolesAllowed(USER)
	public Entry getEvent(String id) {
		return eventSource.getEvent(support.getPrincipal(), id).getEntry(
				abdera, feedLanguage);
	}

	@Override
	public UriBuilder getRunUriBuilder(TavernaRun run) {
		return secure(fromUri(getBaseUriBuilder().path("runs/{uuid}").build(
				run.getId())));
	}

	@Override
	public UriBuilder getBaseUriBuilder() {
		return secure(fromUri(baseURI));
	}

	@Override
	public String resolve(String uri) {
		if (uri == null)
			return null;
		return secure(baseURI, uri).toString();
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		String base = servletContext.getInitParameter(PREFERRED_URI_PARAM);
		if (base == null)
			base = servletContext.getContextPath() + "/rest";
		baseURI = URI.create(base);
	}
}