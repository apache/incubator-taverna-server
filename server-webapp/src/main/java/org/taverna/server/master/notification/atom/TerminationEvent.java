/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification.atom;

import java.net.URI;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.abdera.model.Entry;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * A termination event, suitable for an Atom feed.
 * 
 * @author Donal Fellows
 */
@PersistenceCapable
@XmlType(name = "TerminationEvent", propOrder = {})
@XmlRootElement
public class TerminationEvent extends AbstractEvent {
	/**
	 * Initialize a termination event for a workflow run.
	 * 
	 * @param workflowLink
	 *            A link to the run that created the event.
	 * @param owner
	 *            The identity of the owner of the run.
	 * @param title
	 *            The title of the event.
	 * @param message
	 *            The contents of the event.
	 */
	public TerminationEvent(URI workflowLink, UsernamePrincipal owner,
			String title, String message) {
		super("termination");
		this.owner = owner.getName();
		this.title = title;
		this.message = message;
		this.link = workflowLink.toASCIIString();
	}

	@Persistent
	private String message;
	@Persistent
	private String title;
	@Persistent
	private String link;

	@Override
	public void write(Entry entry, String language) {
		super.write(entry, language);
		entry.setTitle(title).setLanguage(language);
		entry.addLink(link, "related").setTitle("workflow run");
		entry.setContent(message).setLanguage(language);
	}

	@XmlElement
	public String getMessage() {
		return message;
	}

	@XmlElement
	public String getTitle() {
		return title;
	}

	@XmlAttribute
	public String getLink() {
		return link;
	}
}
