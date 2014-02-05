package org.taverna.server.master.identity;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;

public class LoggingAuthProvider implements AuthenticationProvider {
	private AuthenticationProvider delegate;

	@Required
	public void setDelegate(AuthenticationProvider provider) {
		delegate = provider;
	}

	@PerfLogged
	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		return delegate.authenticate(authentication);
	}

	@PerfLogged
	@Override
	public boolean supports(Class<?> authentication) {
		return delegate.supports(authentication);
	}
}
