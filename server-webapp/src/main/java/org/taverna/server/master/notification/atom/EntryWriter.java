/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification.atom;

import org.apache.abdera.model.Entry;
import org.apache.cxf.jaxrs.ext.atom.AtomElementWriter;
import org.springframework.beans.factory.annotation.Required;

/**
 * The thunk that integrates CXF and Abdera with our event feed.
 * 
 * @author Donal Fellows
 */
public class EntryWriter implements AtomElementWriter<Entry, AbstractEvent> {
	private AtomFeed feed;

	@Required
	public void setFeed(AtomFeed feed) {
		this.feed = feed;
	}

	@Override
	public void writeTo(Entry entry, AbstractEvent event) {
		event.write(entry, feed.getFeedLanguage());
	}
}
