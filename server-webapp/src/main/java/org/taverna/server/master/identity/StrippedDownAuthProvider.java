package org.taverna.server.master.identity;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;

/**
 * A stripped down version of a
 * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider
 * DaoAuthenticationProvider}/
 * {@link org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider
 * AbstractUserDetailsAuthenticationProvider} that avoids much of the overhead
 * associated with that class.
 */
public class StrippedDownAuthProvider implements AuthenticationProvider {
	/**
	 * The plaintext password used to perform
	 * {@link PasswordEncoder#isPasswordValid(String, String, Object)} on when
	 * the user is not found to avoid SEC-2056.
	 */
	private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

	/**
	 * The password used to perform
	 * {@link PasswordEncoder#isPasswordValid(String, String, Object)} on when
	 * the user is not found to avoid SEC-2056. This is necessary, because some
	 * {@link PasswordEncoder} implementations will short circuit if the
	 * password is not in a valid format.
	 */
	private String userNotFoundEncodedPassword;
	private UserDetailsService userDetailsService;
	private PasswordEncoder passwordEncoder;
	private Map<String, AuthCacheEntry> authCache = new HashMap<>();
	protected final Log logger = LogFactory.getLog(getClass());

	private static class AuthCacheEntry {
		private String creds;
		private long timestamp;
		private static final long VALIDITY = 1000 * 60 * 20;
		AuthCacheEntry(String credentials) {
			creds = credentials;
			timestamp = System.currentTimeMillis();
		}
		boolean valid(String password) {
			return creds.equals(password) && timestamp+VALIDITY > System.currentTimeMillis();
		}
	}

	@PerfLogged
	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {

		if (!(authentication instanceof UsernamePasswordAuthenticationToken))
			throw new IllegalArgumentException(
					"can only authenticate against username+password");
		UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;

		// Determine username
		String username = (auth.getPrincipal() == null) ? "NONE_PROVIDED"
				: auth.getName();

		UserDetails user;

		try {
			user = retrieveUser(username, auth);
			if (user == null)
				throw new IllegalStateException(
						"retrieveUser returned null - a violation of the interface contract");
		} catch (UsernameNotFoundException notFound) {
			if (logger.isDebugEnabled())
				logger.debug("User '" + username + "' not found", notFound);
			throw new BadCredentialsException("Bad credentials");
		}

		// Pre-auth
		if (!user.isAccountNonLocked())
			throw new LockedException("User account is locked");
		if (!user.isEnabled())
			throw new DisabledException("User account is disabled");
		if (!user.isAccountNonExpired())
			throw new AccountExpiredException("User account has expired");
		Object credentials = auth.getCredentials();
		if (credentials == null) {
			logger.debug("Authentication failed: no credentials provided");

			throw new BadCredentialsException("Bad credentials");
		}

		String providedPassword = credentials.toString();
		boolean matched = false;
		synchronized (authCache) {
			AuthCacheEntry pw = authCache.get(username);
			if (pw != null && providedPassword != null) {
				if (pw.valid(providedPassword))
					matched = true;
				else
					authCache.remove(username);
			}
		}
		// Auth
		if (!matched) {
			if (!passwordEncoder.matches(providedPassword, user.getPassword())) {
				logger.debug("Authentication failed: password does not match stored value");

				throw new BadCredentialsException("Bad credentials");
			}
			if (providedPassword != null)
				synchronized (authCache) {
					authCache.put(username, new AuthCacheEntry(providedPassword));
				}
		}

		// Post-auth
		if (!user.isCredentialsNonExpired())
			throw new CredentialsExpiredException(
					"User credentials have expired");

		return createSuccessAuthentication(user, auth, user);
	}

	@PreDestroy
	void clearCache() {
		authCache.clear();
	}

	/**
	 * Creates a successful {@link Authentication} object.
	 * <p>
	 * Protected so subclasses can override.
	 * </p>
	 * <p>
	 * Subclasses will usually store the original credentials the user supplied
	 * (not salted or encoded passwords) in the returned
	 * <code>Authentication</code> object.
	 * </p>
	 * 
	 * @param principal
	 *            that should be the principal in the returned object (defined
	 *            by the {@link #isForcePrincipalAsString()} method)
	 * @param authentication
	 *            that was presented to the provider for validation
	 * @param user
	 *            that was loaded by the implementation
	 * 
	 * @return the successful authentication token
	 */
	private Authentication createSuccessAuthentication(Object principal,
			Authentication authentication, UserDetails user) {
		/*
		 * Ensure we return the original credentials the user supplied, so
		 * subsequent attempts are successful even with encoded passwords. Also
		 * ensure we return the original getDetails(), so that future
		 * authentication events after cache expiry contain the details
		 */
		UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
				principal, authentication.getCredentials(),
				user.getAuthorities());
		result.setDetails(authentication.getDetails());

		return result;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class
				.isAssignableFrom(authentication);
	}

	/**
	 * Allows subclasses to actually retrieve the <code>UserDetails</code> from
	 * an implementation-specific location, with the option of throwing an
	 * <code>AuthenticationException</code> immediately if the presented
	 * credentials are incorrect (this is especially useful if it is necessary
	 * to bind to a resource as the user in order to obtain or generate a
	 * <code>UserDetails</code>).
	 * <p>
	 * Subclasses are not required to perform any caching, as the
	 * <code>AbstractUserDetailsAuthenticationProvider</code> will by default
	 * cache the <code>UserDetails</code>. The caching of
	 * <code>UserDetails</code> does present additional complexity as this means
	 * subsequent requests that rely on the cache will need to still have their
	 * credentials validated, even if the correctness of credentials was assured
	 * by subclasses adopting a binding-based strategy in this method.
	 * Accordingly it is important that subclasses either disable caching (if
	 * they want to ensure that this method is the only method that is capable
	 * of authenticating a request, as no <code>UserDetails</code> will ever be
	 * cached) or ensure subclasses implement
	 * {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)}
	 * to compare the credentials of a cached <code>UserDetails</code> with
	 * subsequent authentication requests.
	 * </p>
	 * <p>
	 * Most of the time subclasses will not perform credentials inspection in
	 * this method, instead performing it in
	 * {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)}
	 * so that code related to credentials validation need not be duplicated
	 * across two methods.
	 * </p>
	 * 
	 * @param username
	 *            The username to retrieve
	 * @param authentication
	 *            The authentication request, which subclasses <em>may</em> need
	 *            to perform a binding-based retrieval of the
	 *            <code>UserDetails</code>
	 * 
	 * @return the user information (never <code>null</code> - instead an
	 *         exception should the thrown)
	 * 
	 * @throws AuthenticationException
	 *             if the credentials could not be validated (generally a
	 *             <code>BadCredentialsException</code>, an
	 *             <code>AuthenticationServiceException</code> or
	 *             <code>UsernameNotFoundException</code>)
	 */
	private UserDetails retrieveUser(String username,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		try {
			return userDetailsService.loadUserByUsername(username);
		} catch (UsernameNotFoundException notFound) {
			if (authentication.getCredentials() != null) {
				String presentedPassword = authentication.getCredentials()
						.toString();
				passwordEncoder.matches(presentedPassword,
						userNotFoundEncodedPassword);
			}
			throw notFound;
		} catch (AuthenticationException e) {
			throw e;
		} catch (Exception repositoryProblem) {
			throw new AuthenticationServiceException(
					repositoryProblem.getMessage(), repositoryProblem);
		}
	}

	/**
	 * Sets the PasswordEncoder instance to be used to encode and validate
	 * passwords.
	 */
	@Required
	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		if (passwordEncoder == null)
			throw new IllegalArgumentException("passwordEncoder cannot be null");

		this.passwordEncoder = passwordEncoder;
		this.userNotFoundEncodedPassword = passwordEncoder
				.encode(USER_NOT_FOUND_PASSWORD);
	}

	@Required
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		if (userDetailsService == null)
			throw new IllegalStateException("A UserDetailsService must be set");
		this.userDetailsService = userDetailsService;
	}
}
