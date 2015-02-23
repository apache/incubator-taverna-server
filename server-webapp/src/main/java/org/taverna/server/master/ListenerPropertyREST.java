/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.utils.RestUtils.opt;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.taverna.server.master.api.ListenerPropertyBean;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerListenersREST;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

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