/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification;

import static twitter4j.conf.PropertyConfiguration.OAUTH_ACCESS_TOKEN;
import static twitter4j.conf.PropertyConfiguration.OAUTH_ACCESS_TOKEN_SECRET;
import static twitter4j.conf.PropertyConfiguration.OAUTH_CONSUMER_KEY;
import static twitter4j.conf.PropertyConfiguration.OAUTH_CONSUMER_SECRET;

import java.util.Properties;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.auth.AuthorizationFactory;

/**
 * Super simple-minded twitter dispatcher. You need to tell it your consumer key
 * and secret as part of the connection parameters, for example via a dispatcher
 * URN of "<tt>twitter:fred:bloggs</tt>" where <tt>fred</tt> is the key and
 * <tt>bloggs</tt> is the secret.
 * 
 * @author Donal Fellows
 */
public class TwitterDispatcher extends RateLimitedDispatcher {
	public static final int MAX_MESSAGE_LENGTH = 140;
	public static final char ELLIPSIS = '\u2026';

	private String token = "";
	private String secret = "";

	public void setAccessToken(String token) {
		this.token = valid(token, "");
	}

	public void setAccessSecret(String secret) {
		this.secret = valid(secret, "");
	}

	private Properties getConfig() throws NotConfiguredException {
		if (token.isEmpty() || secret.isEmpty())
			throw new NotConfiguredException();
		Properties p = new Properties();
		p.setProperty(ACCESS_TOKEN_PROP, token);
		p.setProperty(ACCESS_SECRET_PROP, secret);
		return p;
	}

	public static final String ACCESS_TOKEN_PROP = OAUTH_ACCESS_TOKEN;
	public static final String ACCESS_SECRET_PROP = OAUTH_ACCESS_TOKEN_SECRET;

	private Twitter getTwitter(String key, String secret) throws Exception {
		if (key.isEmpty() || secret.isEmpty())
			throw new NoCredentialsException();

		Properties p = getConfig();
		p.setProperty(OAUTH_CONSUMER_KEY, key);
		p.setProperty(OAUTH_CONSUMER_SECRET, secret);

		Configuration config = new PropertyConfiguration(p);
		TwitterFactory factory = new TwitterFactory(config);
		Twitter t = factory.getInstance(AuthorizationFactory
				.getInstance(config));
		// Verify that we can connect!
		t.getOAuthAccessToken();
		return t;
	}

	// TODO: Get secret from credential manager
	@Override
	public void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception {
		// messageSubject ignored
		String[] target = targetParameter.split(":", 2);
		if (target == null || target.length != 2)
			throw new Exception("missing consumer key or secret");
		String who = target[0];
		if (!isSendAllowed(who))
			return;
		Twitter twitter = getTwitter(who, target[1]);

		if (messageContent.length() > MAX_MESSAGE_LENGTH)
			messageContent = messageContent
					.substring(0, MAX_MESSAGE_LENGTH - 1) + ELLIPSIS;
		twitter.updateStatus(messageContent);
	}

	@Override
	public boolean isAvailable() {
		try {
			// Try to create the configuration and push it through as far as
			// confirming that we can build an access object (even if it isn't
			// bound to a user)
			new TwitterFactory(new PropertyConfiguration(getConfig()))
					.getInstance();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Indicates that the dispatcher has not been configured with service
	 * credentials.
	 * 
	 * @author Donal Fellows
	 */
	@SuppressWarnings("serial")
	public static class NotConfiguredException extends Exception {
		NotConfiguredException() {
			super("not configured with xAuth key and secret; "
					+ "dispatch not possible");
		}
	}

	/**
	 * Indicates that the user did not supply their credentials.
	 * 
	 * @author Donal Fellows
	 */
	@SuppressWarnings("serial")
	public static class NoCredentialsException extends Exception {
		NoCredentialsException() {
			super("no consumer key and secret present; "
					+ "dispatch not possible");
		}
	}
}
