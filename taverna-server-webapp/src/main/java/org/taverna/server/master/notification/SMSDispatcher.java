/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification;

import static org.taverna.server.master.defaults.Default.SMS_GATEWAY_URL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Required;

/**
 * Dispatch termination messages via SMS.
 * 
 * @author Donal Fellows
 */
public class SMSDispatcher extends RateLimitedDispatcher {
	@Override
	public String getName() {
		return "sms";
	}

	private CloseableHttpClient client;
	private URI service;
	private String user = "", pass = "";
	private String usernameField = "username", passwordField = "password",
			destinationField = "to", messageField = "text";

	/**
	 * @param usernameField
	 *            The name of the field that conveys the sending username; this
	 *            is the <i>server</i>'s identity.
	 */
	@Required
	public void setUsernameField(String usernameField) {
		this.usernameField = usernameField;
	}

	/**
	 * @param passwordField
	 *            The field holding the password to authenticate the server to
	 *            the SMS gateway.
	 */
	@Required
	public void setPasswordField(String passwordField) {
		this.passwordField = passwordField;
	}

	/**
	 * @param destinationField
	 *            The field holding the number to send the SMS to.
	 */
	@Required
	public void setDestinationField(String destinationField) {
		this.destinationField = destinationField;
	}

	/**
	 * @param messageField
	 *            The field holding the plain-text message to send.
	 */
	@Required
	public void setMessageField(String messageField) {
		this.messageField = messageField;
	}

	public void setService(String serviceURL) {
		String s = valid(serviceURL, "");
		if (s.isEmpty()) {
			log.warn("did not get sms.service from servlet config; using default ("
					+ SMS_GATEWAY_URL + ")");
			s = SMS_GATEWAY_URL;
		}
		try {
			service = new URI(s);
		} catch (URISyntaxException e) {
			service = null;
		}
	}

	public void setUser(String user) {
		this.user = valid(user, "");
	}

	public void setPassword(String pass) {
		this.pass = valid(pass, "");
	}

	@PostConstruct
	void init() {
		client = HttpClientBuilder.create().build();
	}

	@PreDestroy
	void close() throws IOException {
		try {
			if (client != null)
				client.close();
		} finally {
			client = null;
		}
	}

	@Override
	public boolean isAvailable() {
		return service != null && !user.isEmpty() && !pass.isEmpty();
	}

	@Override
	public void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception {
		// Sanity check
		if (!targetParameter.matches("[^0-9]+"))
			throw new Exception("invalid phone number");

		if (!isSendAllowed("anyone"))
			return;

		// Build the message to send
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair(usernameField, user));
		params.add(new BasicNameValuePair(passwordField, pass));
		params.add(new BasicNameValuePair(destinationField, targetParameter));
		params.add(new BasicNameValuePair(messageField, messageContent));

		// Send the message
		HttpPost post = new HttpPost(service);
		post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		HttpResponse response = client.execute(post);

		// Log the response
		HttpEntity entity = response.getEntity();
		if (entity != null)
			try (BufferedReader e = new BufferedReader(new InputStreamReader(
					entity.getContent()))) {
				log.info(e.readLine());
			}
	}
}
