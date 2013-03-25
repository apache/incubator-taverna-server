/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.identity;

import static org.taverna.server.master.defaults.Default.AUTHORITY_PREFIX;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Extracts the local user id from the set of Spring Security authorities
 * granted to the current user. This is done by scanning the set of authorities
 * to see if any of them start with the substring listed in the <tt>prefix</tt>
 * property; the username is the rest of the authority string in that case.
 * 
 * @author Donal Fellows
 */
public class AuthorityDerivedIDMapper implements LocalIdentityMapper {
	private String prefix = AUTHORITY_PREFIX;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public String getUsernameForPrincipal(UsernamePrincipal user) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth == null || !auth.isAuthenticated())
			return null;
		for (GrantedAuthority authority : auth.getAuthorities()) {
			String token = authority.getAuthority();
			if (token == null)
				continue;
			if (token.startsWith(prefix))
				return token.substring(prefix.length());
		}
		return null;
	}
}
