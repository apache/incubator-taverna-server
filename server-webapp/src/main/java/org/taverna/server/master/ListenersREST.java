/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.taverna.server.master.common.Uri.secure;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.TavernaServerImpl.SupportAware;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * RESTful interface to a single workflow run's event listeners.
 * 
 * @author Donal Fellows
 */
abstract class ListenersREST implements TavernaServerListenersREST,
		ListenersBean {
	private TavernaRun run;
	private TavernaServerSupport support;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	public ListenersREST connect(TavernaRun run) {
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	public Response addListener(ListenerDefinition typeAndConfiguration,
			UriInfo ui) throws NoUpdateException, NoListenerException {
		String name = support.makeListener(run, typeAndConfiguration.type,
				typeAndConfiguration.configuration).getName();
		return created(secure(ui).path("{listenerName}").build(name)).build();
	}

	@Override
	@CallCounted
	public TavernaServerListenerREST getListener(String name)
			throws NoListenerException {
		Listener l = support.getListener(run, name);
		if (l == null)
			throw new NoListenerException();
		return makeListenerInterface().connect(l, run);
	}

	@NonNull
	protected abstract SingleListenerREST makeListenerInterface();

	@Override
	@CallCounted
	public Listeners getDescription(UriInfo ui) {
		List<ListenerDescription> result = new ArrayList<ListenerDescription>();
		UriBuilder ub = secure(ui).path("{name}");
		for (Listener l : run.getListeners())
			result.add(new ListenerDescription(l,
					fromUri(ub.build(l.getName()))));
		return new Listeners(result, ub);
	}

	@Override
	@CallCounted
	public Response listenersOptions() {
		return opt();
	}
}

/**
 * Description of properties supported by {@link ListenersREST}.
 * 
 * @author Donal Fellows
 */
interface ListenersBean extends SupportAware {
	ListenersREST connect(TavernaRun run);
}