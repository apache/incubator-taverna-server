/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import java.io.Serializable;
import java.security.Principal;

import org.taverna.server.master.interfaces.TavernaSecurityContext;

public interface SecurityContextFactory extends Serializable {
	/**
	 * Creates a security context.
	 * 
	 * @param run
	 *            Handle to remote run. Allows the security context to know how
	 *            to apply itself to the workflow run.
	 * @param owner
	 *            The identity of the owner of the workflow run.
	 * @return The security context.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	TavernaSecurityContext create(RemoteRunDelegate run, Principal owner)
			throws Exception;
}
