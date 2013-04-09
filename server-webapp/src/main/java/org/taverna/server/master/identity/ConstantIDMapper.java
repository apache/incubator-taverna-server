/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.identity;

import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * A trivial principal to user mapper that always uses the same ID.
 * @author Donal Fellows
 */
public class ConstantIDMapper implements LocalIdentityMapper {
	private String id;

	/**
	 * Sets what local user ID all users should be mapped to.
	 * 
	 * @param id
	 *            The local user ID.
	 */
	public void setConstantId(String id) {
		this.id = id;
	}

	@Override
	public String getUsernameForPrincipal(UsernamePrincipal user) {
		return id;
	}
}
