/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static twitter4j.conf.PropertyConfiguration.OAUTH_ACCESS_TOKEN;
import static twitter4j.conf.PropertyConfiguration.OAUTH_ACCESS_TOKEN_SECRET;
import static twitter4j.conf.PropertyConfiguration.OAUTH_CONSUMER_KEY;
import static twitter4j.conf.PropertyConfiguration.OAUTH_CONSUMER_SECRET;

import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.taverna.server.master.interfaces.MessageDispatcher;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.http.AuthorizationFactory;

/**
 * Super simple-minded twitter dispatcher. You need to tell it your consumer key
 * and secret as part of the connection parameters, for example via a dispatcher
 * URN of "<tt>twitter:fred:bloggs</tt>" where <tt>fred</tt> is the key and
 * <tt>bloggs</tt> is the secret.
 * 
 * @author Donal Fellows
 */
public class TwitterDispatcher implements MessageDispatcher {
	public static final int MAX_MESSAGE_LENGTH = 140;
	public static final char ELLIPSIS = 8230;

	private Properties props = new Properties();

	public void setProperties(Properties properties) {
		props = properties;
		synchronized (this) {
			factory = null;
		}
	}

	private TwitterFactory factory;
	public static final String ACCESS_TOKEN_PROP = OAUTH_ACCESS_TOKEN;
	public static final String ACCESS_SECRET_PROP = OAUTH_ACCESS_TOKEN_SECRET;

	@Context
	private ServletConfig config;

	private Twitter getTwitter(String key, String secret) throws Exception {
		Properties p = (Properties) props.clone();

		if (config != null) {
			String str;
			str = config.getInitParameter(ACCESS_TOKEN_PROP);
			if (str != null)
				p.setProperty(ACCESS_TOKEN_PROP, str);
			str = config.getInitParameter(ACCESS_SECRET_PROP);
			if (str != null)
				p.setProperty(ACCESS_SECRET_PROP, str);
		}
		if (p.getProperty(ACCESS_TOKEN_PROP, "").isEmpty()
				|| p.getProperty(ACCESS_SECRET_PROP, "").isEmpty())
			throw new NotConfiguredException();
		p.setProperty(OAUTH_CONSUMER_KEY, key);
		p.setProperty(OAUTH_CONSUMER_SECRET, secret);
		if (key.isEmpty() || secret.isEmpty())
			throw new NoCredentialsException();
		Configuration config = new PropertyConfiguration(p);
		factory = new TwitterFactory(config);
		Twitter t = factory.getInstance(AuthorizationFactory
				.getInstance(config));
		// Verify that we can connect!
		t.getOAuthAccessToken();
		return t;
	}

	// TODO: Get password from credential manager
	@Override
	public void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception {
		// messageSubject ignored
		String[] target = targetParameter.split(":", 2);
		if (target == null || target.length != 2)
			throw new Exception("missing consumer key or secret");
		Twitter twitter = getTwitter(target[0], target[1]);

		if (messageContent.length() > MAX_MESSAGE_LENGTH)
			messageContent = messageContent
					.substring(0, MAX_MESSAGE_LENGTH - 1) + ELLIPSIS;
		twitter.updateStatus(messageContent);
	}

	public static class NotConfiguredException extends Exception {
		NotConfiguredException() {
			super("not configured with xAuth key and secret; "
					+ "dispatch not possible");
		}
	}

	public static class NoCredentialsException extends Exception {
		NoCredentialsException() {
			super("no consumer key and secret present; "
					+ "dispatch not possible");
		}
	}
}
