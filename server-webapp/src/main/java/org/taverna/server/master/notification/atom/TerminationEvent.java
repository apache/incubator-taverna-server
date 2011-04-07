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

@PersistenceCapable
@XmlType
@XmlRootElement
public class TerminationEvent extends AbstractEvent {
	public TerminationEvent(URI workflowLink, UsernamePrincipal owner,
			String title, String message) {
		super("termination");
		this.owner = owner.getName();
		this.title = title;
		this.message = message;
		this.link = workflowLink.toASCIIString();
	}

	@Persistent
	@XmlElement
	private String message;
	@Persistent
	@XmlElement
	private String title;
	@Persistent
	@XmlAttribute
	private String link;

	@Override
	public void write(Entry entry) {
		super.write(entry);
		entry.setTitle(title);
		entry.setText(message);
		entry.addLink(link, "workflowRun");
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
}
