package org.taverna.server.master.notification.atom;

import org.apache.abdera.model.Entry;
import org.apache.cxf.jaxrs.ext.atom.AtomElementWriter;

public class EntryWriter implements AtomElementWriter<Entry, AbstractEvent> {
	@Override
	public void writeTo(Entry entry, AbstractEvent event) {
		event.write(entry);
	}
}
