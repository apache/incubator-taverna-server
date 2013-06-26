/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.identity;

import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;
import static org.taverna.server.master.common.Roles.SELF;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.taverna.server.master.worker.RunDatabaseDAO;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A special authentication provider that allows a workflow to authenticate to
 * itself. This is used to allow the workflow to publish to its own interaction
 * feed.
 * 
 * @author Donal Fellows
 */
public class WorkflowInternalAuthProvider extends
		AbstractUserDetailsAuthenticationProvider {
	private static final Log log = LogFactory.getLog("Taverna.Server.UserDB");
	private static final boolean logDecisions = true;
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

	@PostConstruct
	public void logConfig() {
		log.info("authorized addresses for automatic access: "
				+ authorizedAddresses);
	}

	private final Set<String> localAddresses = new HashSet<String>();
	private Set<String> authorizedAddresses;
	{
		localAddresses.add("127.0.0.1"); // IPv4
		localAddresses.add("::1"); // IPv6
		try {
			InetAddress addr = InetAddress.getLocalHost();
			if (!addr.isLoopbackAddress())
				localAddresses.add(addr.getHostAddress());
		} catch (UnknownHostException e) {
			// Ignore the exception
		}
		authorizedAddresses = new HashSet<String>(localAddresses);
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userRecord,
			UsernamePasswordAuthenticationToken token)
			throws AuthenticationException {
		WebAuthenticationDetails wad = (WebAuthenticationDetails) token
				.getDetails();
		HttpServletRequest req = ((ServletRequestAttributes) getRequestAttributes())
				.getRequest();

		// Are we coming from a "local" address?
		if (!req.getLocalAddr().equals(wad.getRemoteAddress())
				&& !authorizedAddresses.contains(wad.getRemoteAddress())) {
			if (logDecisions)
				log.info("attempt to use workflow magic token from untrusted address:"
						+ " token="
						+ userRecord.getUsername()
						+ ", address="
						+ wad.getRemoteAddress());
			throw new BadCredentialsException("bad login token");
		}

		// Does the password match?
		if (!token.getCredentials().equals(userRecord.getPassword())) {
			if (logDecisions)
				log.info("workflow magic token is untrusted due to password mismatch:"
						+ " wanted="
						+ userRecord.getPassword()
						+ ", got="
						+ token.getCredentials());
			throw new BadCredentialsException("bad login token");
		}

		if (logDecisions)
			log.info("granted role " + SELF + " to user "
					+ userRecord.getUsername());
	}

	@Override
	@NonNull
	protected UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken token)
			throws AuthenticationException {
		if (token.getDetails() == null
				|| !(token.getDetails() instanceof WebAuthenticationDetails))
			throw new UsernameNotFoundException("context unsupported");
		if (!username.startsWith(PREFIX))
			throw new UsernameNotFoundException(
					"unsupported username for this provider");
		if (logDecisions)
			log.info("request for auth for user " + username);
		String wfid = username.substring(PREFIX.length());
		String securityToken = dao.getSecurityToken(wfid);
		if (securityToken == null)
			throw new UsernameNotFoundException("no such user");
		return new User(username, securityToken, true, true, true, true,
				Arrays.asList(new LiteralGrantedAuthority(SELF),
						new WorkflowSelfAuthority(wfid)));
	}

	@SuppressWarnings("serial")
	public static class WorkflowSelfAuthority extends LiteralGrantedAuthority {
		public WorkflowSelfAuthority(String wfid) {
			super(wfid);
		}

		public String getWorkflowID() {
			return getAuthority();
		}

		@Override
		public String toString() {
			return "WORKFLOW(" + getAuthority() + ")";
		}
	}
}
