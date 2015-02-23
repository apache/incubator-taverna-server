/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification.atom;

import static java.util.UUID.randomUUID;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.joda.time.DateTime;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Parent class of all events that may appear on the feed for a workflow run.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
@PersistenceCapable(schema = "ATOM", table = "EVENTS")
@Queries({
		@Query(name = "eventsForUser", language = "SQL", value = "SELECT id FROM ATOM.EVENTS WHERE owner = ? ORDER BY published DESC", resultClass = String.class),
		@Query(name = "eventForUserAndId", language = "SQL", value = "SELECT id FROM ATOM.EVENTS WHERE owner = ? AND id = ?", resultClass = String.class),
		@Query(name = "eventsFromBefore", language = "SQL", value = "SELECT id FROM ATOM.EVENTS where published < ?", resultClass = String.class) })
public class Event implements Serializable {
	@Persistent(primaryKey = "true")
	@Column(length = 48)
	private String id;
	@Persistent
	private String owner;
	@Persistent
	@Index
	private Date published;
	@Persistent
	private String message;
	@Persistent
	private String title;
	@Persistent
	private String link;

	Event() {
	}

	/**
	 * Initialise the identity of this event and the point at which it was
	 * published.
	 * 
	 * @param idPrefix
	 *            A prefix for the identity of this event.
	 * @param owner
	 *            Who is the owner of this event.
	 */
	Event(String idPrefix, URI workflowLink, UsernamePrincipal owner,
			String title, String message) {
		id = idPrefix + "." + randomUUID().toString();
		published = new Date();
		this.owner = owner.getName();
		this.title = title;
		this.message = message;
		this.link = workflowLink.toASCIIString();
	}

	public final String getId() {
		return id;
	}

	public final String getOwner() {
		return owner;
	}

	public final DateTime getPublished() {
		return new DateTime(published);
	}

	public String getMessage() {
		return message;
	}

	public String getTitle() {
		return title;
	}

	public String getLink() {
		return link;
	}

	public Entry getEntry(Abdera abdera, String language) {
		Entry entry = abdera.getFactory().newEntry();
		entry.setId(id);
		entry.setPublished(published);
		entry.addAuthor(owner).setLanguage(language);
		entry.setUpdated(published);
		entry.setTitle(title).setLanguage(language);
		entry.addLink(link, "related").setTitle("workflow run");
		entry.setContent(message).setLanguage(language);
		return entry;
	}
}
