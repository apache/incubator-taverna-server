package org.taverna.server.master.notification.atom;

import java.io.Serializable;
import java.util.Date;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.joda.time.DateTime;

public abstract class AbstractEvent implements Serializable {
	public String owner;
	public String id;
	public DateTime published;

	public void write(Feed feed) {
		write(feed.addEntry());
	}

	public void write(Entry entry) {
		entry.setId(id);
		entry.setPublished(published.toDate());
		entry.addAuthor(owner);
		entry.setUpdated(new Date());
	}
}
