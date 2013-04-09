/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * This interface describes how to map from the identity understood by the
 * webapp to the identity understood by the local execution system.
 * 
 * @author Donal Fellows
 */
public interface LocalIdentityMapper {
	/**
	 * Given a user's identity, get the local identity to use for executing
	 * their workflows. Note that it is assumed that there will never be a
	 * failure from this interface; it is <i>not</i> a security policy
	 * decision or enforcement point.
	 * 
	 * @param user
	 *            An identity token.
	 * @return A user name, which must be defined in the context that workflows
	 *         will be running in.
	 */
	public String getUsernameForPrincipal(UsernamePrincipal user);
}
