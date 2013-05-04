/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.identity;

import static org.taverna.server.master.common.Roles.SELF;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.taverna.server.master.worker.RunDatabaseDAO;

/**
 * A special authentication provider that allows a workflow to authenticate to
 * itself. This is used to allow the workflow to publish to its own interaction
 * feed.
 * 
 * @author Donal Fellows
 */
public class WorkflowInternalAuthProvider extends
		AbstractUserDetailsAuthenticationProvider {
	public static final String PREFIX = "wfrun_";
	private RunDatabaseDAO dao;

	@Required
	public void setDao(RunDatabaseDAO dao) {
		this.dao = dao;
	}

	public void setAuthorizedAddresses(String[] addresses) {
		authorizedAddresses = new HashSet<String>(localAddresses);
		for (String s : addresses)
			authorizedAddresses.add(s);
	}

	private final Set<String> localAddresses = new HashSet<String>();
	private Set<String> authorizedAddresses;
	{
		localAddresses.add("127.0.0.1");
		localAddresses.add("localhost");
		localAddresses.add("::1");
		authorizedAddresses = new HashSet<String>(localAddresses);
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		if (!authentication.getCredentials().equals(userDetails.getPassword()))
			throw new BadCredentialsException("bad login token");
	}

	@Override
	protected UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		if (authentication.getDetails() == null
				|| !(authentication.getDetails() instanceof WebAuthenticationDetails))
			throw new UsernameNotFoundException("context unsupported");
		WebAuthenticationDetails wad = (WebAuthenticationDetails) authentication
				.getDetails();
		if (!authorizedAddresses.contains(wad.getRemoteAddress()))
			throw new UsernameNotFoundException(
					"provider unsupported in this context");
		if (!username.startsWith(PREFIX))
			throw new UsernameNotFoundException(
					"unsupported username for this provider");
		String token = dao.getSecurityToken(username.substring(PREFIX.length()));
		if (token == null)
			throw new UsernameNotFoundException("no such user");
		return new org.springframework.security.core.userdetails.User(username,
				token, true, true, true, true, Arrays.asList(
						new LiteralGrantedAuthority(SELF),
						new LiteralGrantedAuthority(username)));
	}
}
