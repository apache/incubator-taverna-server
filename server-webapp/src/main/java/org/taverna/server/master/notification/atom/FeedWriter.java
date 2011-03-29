package org.taverna.server.master.notification.atom;

import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.atom.AtomElementWriter;

public class FeedWriter implements AtomElementWriter<Feed, AbstractEvent> {
	@Override
	public void writeTo(Feed feed, AbstractEvent event) {
		event.write(feed);
	}
}
