/*
 */
package org.taverna.server.master;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.taverna.server.master.common.Uri.secure;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.api.ListenersBean;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.ListenerDefinition;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

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
	@PerfLogged
	public Response addListener(ListenerDefinition typeAndConfiguration,
			UriInfo ui) throws NoUpdateException, NoListenerException {
		String name = support.makeListener(run, typeAndConfiguration.type,
				typeAndConfiguration.configuration).getName();
		return created(secure(ui).path("{listenerName}").build(name)).build();
	}

	@Override
	@CallCounted
	@PerfLogged
	public TavernaServerListenerREST getListener(String name)
			throws NoListenerException {
		Listener l = support.getListener(run, name);
		if (l == null)
			throw new NoListenerException();
		return makeListenerInterface().connect(l, run);
	}

	@Nonnull
	protected abstract SingleListenerREST makeListenerInterface();

	@Override
	@CallCounted
	@PerfLogged
	public Listeners getDescription(UriInfo ui) {
		List<ListenerDescription> result = new ArrayList<>();
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