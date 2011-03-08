/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static org.taverna.server.master.TavernaServerImpl.log;

import org.taverna.server.master.TavernaServerImpl.WebappAware;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerListenersREST;

/**
 * RESTful interface to a single property of a workflow run.
 * 
 * @author Donal Fellows
 */
abstract class ListenerPropertyREST implements TavernaServerListenersREST.Property,
		WebappAware {
	private TavernaServer webapp;
	private Listener listen;
	private String propertyName;
	private TavernaRun run;

	@Override
	public void setWebapp(TavernaServer webapp) {
		this.webapp = webapp;
	}

	void setListen(Listener listen) {
		this.listen = listen;
	}

	void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	void setRun(TavernaRun run) {
		this.run = run;
	}

	@Override
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
	public String setValue(String value) throws NoUpdateException,
			NoListenerException {
		webapp.permitUpdate(run);
		listen.setProperty(propertyName, value);
		return listen.getProperty(propertyName);
	}
}