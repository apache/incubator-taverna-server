/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification.atom;

import static java.util.UUID.randomUUID;

import java.io.Serializable;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.joda.time.DateTime;

@PersistenceCapable(schema = "ATOM", table = "EVENTS")
@Queries({
		@Query(name = "eventsForUser", language = "SQL", value = "SELECT id FROM ATOM.EVENTS WHERE owner = ? ORDER BY published DESC", resultClass = String.class),
		@Query(name = "eventForUserAndId", language = "SQL", value = "SELECT id FROM ATOM.EVENTS WHERE owner = ? AND id = ?", resultClass = String.class),
		@Query(name = "eventsFromBefore", language = "SQL", value = "SELECT id FROM ATOM.EVENTS where published < ?", resultClass = String.class) })
@XmlType
@XmlSeeAlso(TerminationEvent.class)
public abstract class AbstractEvent implements Serializable {
	@Persistent(primaryKey = "true")
	@Column(length = 48)
	@XmlAttribute
	private String id;
	@Persistent
	@XmlTransient
	protected String owner;
	@Persistent
	@Index
	@XmlAttribute
	protected Date published;

	protected AbstractEvent(String idPrefix) {
		id = idPrefix + "." + randomUUID().toString();
		published = new Date();
	}

	public void write(Feed feed) {
		write(feed.addEntry());
	}

	public void write(Entry entry) {
		entry.setId(id);
		entry.setPublished(published);
		entry.addAuthor(owner);
		entry.setUpdated(new Date());
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
}
