package org.taverna.server.master.utils;

import java.io.Serializable;
import java.security.Principal;

/**
 * A simple serializable principal that just records the name.
 * 
 * @author Donal Fellows
 */
public class UsernamePrincipal implements Principal, Serializable {
	private static final long serialVersionUID = 2703493248562435L;
	public UsernamePrincipal(String username) {
		this.name = username;
	}

	public UsernamePrincipal(Principal other) {
		this.name = other.getName();
	}

	String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Principal<" + name + ">";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Principal) {
			Principal p = (Principal) o;
			return name.equals(p.getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
