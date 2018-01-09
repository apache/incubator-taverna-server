/*
 */
package org.apache.taverna.server.master;
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

import static java.util.Arrays.asList;
import static org.apache.taverna.server.master.common.Uri.secure;
import static org.apache.taverna.server.master.utils.RestUtils.opt;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.taverna.server.master.api.OneListenerBean;
import org.apache.taverna.server.master.exceptions.NoListenerException;
import org.apache.taverna.server.master.interfaces.Listener;
import org.apache.taverna.server.master.interfaces.TavernaRun;
import org.apache.taverna.server.master.rest.TavernaServerListenersREST;
import org.apache.taverna.server.master.rest.TavernaServerListenersREST.ListenerDescription;
import org.apache.taverna.server.master.rest.TavernaServerListenersREST.TavernaServerListenerREST;
import org.apache.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.apache.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * RESTful interface to a single listener attached to a workflow run.
 * 
 * @author Donal Fellows
 */
abstract class SingleListenerREST implements TavernaServerListenerREST,
		OneListenerBean {
	private Listener listen;
	private TavernaRun run;

	@Override
	public SingleListenerREST connect(Listener listen, TavernaRun run) {
		this.listen = listen;
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	@PerfLogged
	public String getConfiguration() {
		return listen.getConfiguration();
	}

	@Override
	@CallCounted
	@PerfLogged
	public ListenerDescription getDescription(UriInfo ui) {
		return new ListenerDescription(listen, secure(ui));
	}

	@Override
	@CallCounted
	@PerfLogged
	public TavernaServerListenersREST.Properties getProperties(UriInfo ui) {
		return new TavernaServerListenersREST.Properties(secure(ui).path(
				"{prop}"), listen.listProperties());
	}

	@Override
	@CallCounted
	@PerfLogged
	public TavernaServerListenersREST.Property getProperty(
			final String propertyName) throws NoListenerException {
		List<String> p = asList(listen.listProperties());
		if (p.contains(propertyName)) {
			return makePropertyInterface().connect(listen, run, propertyName);
		}
		throw new NoListenerException("no such property");
	}

	protected abstract ListenerPropertyREST makePropertyInterface();

	@Override
	@CallCounted
	public Response listenerOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response configurationOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response propertiesOptions() {
		return opt();
	}
}
