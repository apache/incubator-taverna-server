/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import org.taverna.server.master.notification.atom.EventDAO;

/**
 * What the remote run really needs of its factory.
 * 
 * @author Donal Fellows
 */
public interface FactoryBean {
	/**
	 * @return Whether a run can actually be started at this time.
	 */
	boolean isAllowingRunsToStart();

	/**
	 * @return a handle to the master Atom event feed (<i>not</i> the per-run
	 *         feed)
	 */
	EventDAO getMasterEventFeed();
}
