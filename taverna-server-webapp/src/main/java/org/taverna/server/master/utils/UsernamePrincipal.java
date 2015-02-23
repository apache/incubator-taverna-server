/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import java.io.Serializable;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

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

	public UsernamePrincipal(Authentication auth) {
		this(auth.getPrincipal());
	}

	public UsernamePrincipal(Object principal) {
		if (principal instanceof Principal)
			this.name = ((Principal) principal).getName();
		else if (principal instanceof String)
			this.name = (String) principal;
		else if (principal instanceof UserDetails)
			this.name = ((UserDetails) principal).getUsername();
		else
			this.name = principal.toString();
	}

	private String name;

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
