/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.TavernaServerImpl.WebappAware;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.TavernaServerListenersREST;

/**
 * RESTful interface to a single workflow run's event listeners.
 * 
 * @author Donal Fellows
 */
abstract class ListenersREST implements TavernaServerListenersREST, WebappAware {
	private TavernaRun run;
	private TavernaServer webapp;

	@Override
	public void setWebapp(TavernaServer webapp) {
		this.webapp = webapp;
	}

	void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
	public Response addListener(ListenerDefinition typeAndConfiguration,
			UriInfo ui) throws NoUpdateException, NoListenerException {
		String name = webapp.makeListener(run, typeAndConfiguration.type,
				typeAndConfiguration.configuration).getName();
		return created(
				ui.getAbsolutePathBuilder().path("{listenerName}").build(name))
				.build();
	}

	@Override
	public TavernaServerListenerREST getListener(String name)
			throws NoListenerException {
		Listener l = TavernaServerImpl.getListener(run, name);
		if (l == null)
			throw new NoListenerException();
		SingleListenerREST listener = makeListenerInterface();
		listener.setRun(run);
		listener.setListen(l);
		return listener;
	}

	protected abstract SingleListenerREST makeListenerInterface();

	@Override
	public Listeners getDescription(UriInfo ui) {
		List<ListenerDescription> result = new ArrayList<ListenerDescription>();
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");
		for (Listener l : run.getListeners()) {
			URI base = ub.build(l.getName());
			result.add(new ListenerDescription(l, fromUri(base)));
		}
		return new Listeners(result, ub);
	}
}