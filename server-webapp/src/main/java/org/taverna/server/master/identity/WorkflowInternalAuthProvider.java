/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.identity;

import static java.util.Collections.synchronizedMap;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;
import static org.taverna.server.master.common.Roles.SELF;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.UsernamePrincipal;
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
	private static Log log = LogFactory.getLog("Taverna.Server.UserDB");
	private static final boolean logDecisions = true;
	public static final String PREFIX = "wfrun_";
	private RunDatabaseDAO dao;
	private Map<String, String> cache;

	@Required
	public void setDao(RunDatabaseDAO dao) {
		this.dao = dao;
	}

	@Required
	@SuppressWarnings("serial")
	public void setCacheBound(final int bound) {
		cache = synchronizedMap(new LinkedHashMap<String, String>() {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
				return size() > bound;
			}
		});
	}

	public void setAuthorizedAddresses(String[] addresses) {
		authorizedAddresses = new HashSet<>(localAddresses);
		for (String s : addresses)
			authorizedAddresses.add(s);
	}

	@PostConstruct
	public void logConfig() {
		log.info("authorized addresses for automatic access: "
				+ authorizedAddresses);
	}

	@PreDestroy
	void closeLog() {
		log = null;
	}

	private final Set<String> localAddresses = new HashSet<>();
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
		authorizedAddresses = new HashSet<>(localAddresses);
	}

	/**
	 * Check that the authentication request is actually valid for the given
	 * user record.
	 * 
	 * @param userRecord
	 *            as retrieved from the
	 *            {@link #retrieveUser(String, UsernamePasswordAuthenticationToken)}
	 *            or <code>UserCache</code>
	 * @param principal
	 *            the principal that is trying to authenticate (and that we're
	 *            trying to bind)
	 * @param credentials
	 *            the credentials (e.g., password) presented by the principal
	 * 
	 * @throws AuthenticationException
	 *             AuthenticationException if the credentials could not be
	 *             validated (generally a <code>BadCredentialsException</code>,
	 *             an <code>AuthenticationServiceException</code>)
	 * @throws Exception
	 *             If something goes wrong. Will be logged and converted to a
	 *             generic AuthenticationException.
	 */
	protected void additionalAuthenticationChecks(UserDetails userRecord,
			@Nonnull Object principal, @Nonnull Object credentials)
			throws Exception {
		@Nonnull
		HttpServletRequest req = ((ServletRequestAttributes) currentRequestAttributes())
				.getRequest();

		// Are we coming from a "local" address?
		if (!req.getLocalAddr().equals(req.getRemoteAddr())
				&& !authorizedAddresses.contains(req.getRemoteAddr())) {
			if (logDecisions)
				log.info("attempt to use workflow magic token from untrusted address:"
						+ " token="
						+ userRecord.getUsername()
						+ ", address="
						+ req.getRemoteAddr());
			throw new BadCredentialsException("bad login token");
		}

		// Does the password match?
		if (!credentials.equals(userRecord.getPassword())) {
			if (logDecisions)
				log.info("workflow magic token is untrusted due to password mismatch:"
						+ " wanted="
						+ userRecord.getPassword()
						+ ", got="
						+ credentials);
			throw new BadCredentialsException("bad login token");
		}

		if (logDecisions)
			log.info("granted role " + SELF + " to user "
					+ userRecord.getUsername());
	}

	/**
	 * Retrieve the <code>UserDetails</code> from the relevant store, with the
	 * option of throwing an <code>AuthenticationException</code> immediately if
	 * the presented credentials are incorrect (this is especially useful if it
	 * is necessary to bind to a resource as the user in order to obtain or
	 * generate a <code>UserDetails</code>).
	 * 
	 * @param username
	 *            The username to retrieve
	 * @param details
	 *            The details from the authentication request.
	 * @see #retrieveUser(String,UsernamePasswordAuthenticationToken)
	 * @return the user information (never <code>null</code> - instead an
	 *         exception should the thrown)
	 * @throws AuthenticationException
	 *             if the credentials could not be validated (generally a
	 *             <code>BadCredentialsException</code>, an
	 *             <code>AuthenticationServiceException</code> or
	 *             <code>UsernameNotFoundException</code>)
	 * @throws Exception
	 *             If something goes wrong. It will be logged and converted into
	 *             a general AuthenticationException.
	 */
	@Nonnull
	protected UserDetails retrieveUser(String username, Object details)
			throws Exception {
		if (details == null || !(details instanceof WebAuthenticationDetails))
			throw new UsernameNotFoundException("context unsupported");
		if (!username.startsWith(PREFIX))
			throw new UsernameNotFoundException(
					"unsupported username for this provider");
		if (logDecisions)
			log.info("request for auth for user " + username);
		String wfid = username.substring(PREFIX.length());
		String securityToken;
		try {
			securityToken = cache.get(wfid);
			if (securityToken == null) {
				securityToken = dao.getSecurityToken(wfid);
				if (securityToken == null)
					throw new UsernameNotFoundException("no such user");
				cache.put(wfid, securityToken);
			}
		} catch (NullPointerException npe) {
			throw new UsernameNotFoundException("no such user");
		}
		return new User(username, securityToken, true, true, true, true,
				Arrays.asList(new LiteralGrantedAuthority(SELF),
						new WorkflowSelfAuthority(wfid)));
	}

	@Override
	@PerfLogged
	protected final void additionalAuthenticationChecks(UserDetails userRecord,
			UsernamePasswordAuthenticationToken token) {
		try {
			additionalAuthenticationChecks(userRecord, token.getPrincipal(),
					token.getCredentials());
		} catch (AuthenticationException e) {
			throw e;
		} catch (Exception e) {
			log.warn("unexpected failure in authentication", e);
			throw new AuthenticationServiceException(
					"unexpected failure in authentication", e);
		}
	}

	@Override
	@Nonnull
	@PerfLogged
	protected final UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken token) {
		try {
			return retrieveUser(username, token.getDetails());
		} catch (AuthenticationException e) {
			throw e;
		} catch (Exception e) {
			log.warn("unexpected failure in authentication", e);
			throw new AuthenticationServiceException(
					"unexpected failure in authentication", e);
		}
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

	public static class WorkflowSelfIDMapper implements LocalIdentityMapper {
		private static Log log = LogFactory.getLog("Taverna.Server.UserDB");
		private RunStore runStore;

		@PreDestroy
		void closeLog() {
			log = null;
		}

		@Required
		public void setRunStore(RunStore runStore) {
			this.runStore = runStore;
		}

		private String getUsernameForSelfAccess(WorkflowSelfAuthority authority)
				throws UnknownRunException {
			return runStore.getRun(authority.getWorkflowID())
					.getSecurityContext().getOwner().getName();
		}

		@Override
		@PerfLogged
		public String getUsernameForPrincipal(UsernamePrincipal user) {
			Authentication auth = SecurityContextHolder.getContext()
					.getAuthentication();
			if (auth == null || !auth.isAuthenticated())
				return null;
			try {
				for (GrantedAuthority authority : auth.getAuthorities())
					if (authority instanceof WorkflowSelfAuthority)
						return getUsernameForSelfAccess((WorkflowSelfAuthority) authority);
			} catch (UnknownRunException e) {
				log.warn("workflow run disappeared during computation of workflow map identity");
			}
			return null;
		}
	}
}
