/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static java.util.Arrays.asList;

import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.rest.TavernaServerListenersREST.ListenerDescription;
import org.taverna.server.master.rest.TavernaServerListenersREST.TavernaServerListenerREST;

/**
 * RESTful interface to a single listener attached to a workflow run.
 * 
 * @author Donal Fellows
 */
abstract class SingleListenerREST implements TavernaServerListenerREST {
	private Listener listen;
	private TavernaRun run;

	void setListen(Listener listen) {
		this.listen = listen;
	}

	void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
	public String getConfiguration() {
		return listen.getConfiguration();
	}

	@Override
	public ListenerDescription getDescription(UriInfo ui) {
		return new ListenerDescription(listen, ui.getAbsolutePathBuilder());
	}

	@Override
	public TavernaServerListenersREST.Properties getProperties(UriInfo ui) {
		return new TavernaServerListenersREST.Properties(ui
				.getAbsolutePathBuilder().path("{prop}"),
				listen.listProperties());
	}

	@Override
	public TavernaServerListenersREST.Property getProperty(
			final String propertyName) throws NoListenerException {
		List<String> p = asList(listen.listProperties());
		if (p.contains(propertyName)) {
			ListenerPropertyREST prop = makePropertyInterface();
			prop.setRun(run);
			prop.setListen(listen);
			prop.setPropertyName(propertyName);
			return prop;
		}
		throw new NoListenerException("no such property");
	}

	protected abstract ListenerPropertyREST makePropertyInterface();
}