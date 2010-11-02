package org.taverna.server.master;

import java.security.Principal;

import org.taverna.server.master.interfaces.LocalIdentityMapper;

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

	/**
	 * Gets what local user ID all users should be mapped to.
	 * 
	 * @return The local user ID.
	 */
	public String getConstantID() {
		return id;
	}

	@Override
	public String getUsernameForPrincipal(Principal user) {
		return id;
	}
}
