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

import static org.apache.commons.logging.LogFactory.getLog;
import static org.apache.taverna.server.master.utils.RestUtils.opt;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.taverna.server.master.api.ListenerPropertyBean;
import org.apache.taverna.server.master.exceptions.NoListenerException;
import org.apache.taverna.server.master.exceptions.NoUpdateException;
import org.apache.taverna.server.master.interfaces.Listener;
import org.apache.taverna.server.master.interfaces.TavernaRun;
import org.apache.taverna.server.master.rest.TavernaServerListenersREST;
import org.apache.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.apache.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * RESTful interface to a single property of a workflow run.
 * 
 * @author Donal Fellows
 */
class ListenerPropertyREST implements TavernaServerListenersREST.Property,
		ListenerPropertyBean {
	private Log log = getLog("Taverna.Server.Webapp");
	private TavernaServerSupport support;
	private Listener listen;
	private String propertyName;
	private TavernaRun run;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	public ListenerPropertyREST connect(Listener listen, TavernaRun run,
			String propertyName) {
		this.listen = listen;
		this.propertyName = propertyName;
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	@PerfLogged
	public String getValue() {
		try {
			return listen.getProperty(propertyName);
		} catch (NoListenerException e) {
			log.error("unexpected exception; property \"" + propertyName
					+ "\" should exist", e);
			return null;
		}
	}

	@Override
	@CallCounted
	@PerfLogged
	public String setValue(String value) throws NoUpdateException,
			NoListenerException {
		support.permitUpdate(run);
		listen.setProperty(propertyName, value);
		return listen.getProperty(propertyName);
	}

	@Override
	@CallCounted
	public Response options() {
		return opt("PUT");
	}
}