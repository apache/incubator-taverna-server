package org.taverna.server.master.notification.atom;

import static java.util.UUID.randomUUID;

import java.net.URI;

import org.apache.abdera.model.Entry;
import org.joda.time.DateTime;

public class TerminationEvent extends AbstractEvent {
	public TerminationEvent(URI workflowLink, String owner, String title, String message) {
		this.owner = owner;
		this.id = "termination" + randomUUID();
		this.title = title;
		this.message = message;
		this.published = new DateTime();
		this.link = workflowLink.toASCIIString();
	}

	private String message;
	private String title;
	private String link;

	@Override
	public void write(Entry entry) {
		super.write(entry);
		entry.setTitle(title);
		entry.setText(message);
		entry.addLink(link, "workflow");
	}
}
