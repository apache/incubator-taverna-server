package org.taverna.server.master.notification;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * Dispatch termination messages via SMS.
 * 
 * @author Donal Fellows
 */
public class SMSDispatcher extends RateLimitedDispatcher {
	public SMSDispatcher() {
		super("sms");
	}

	/** The address of the SMS gateway service used by default. */
	public static final String DEFAULT_SMS_GATEWAY = "https://www.intellisoftware.co.uk/smsgateway/sendmsg.aspx";
	private HttpClient client;
	private URI service;
	private String user = "", pass = "";
	private String usernameField = "username", passwordField = "password",
			destinationField = "to", messageField = "text";

	/**
	 * @param usernameField
	 *            The name of the field that conveys the sending username; this
	 *            is the <i>server</i>'s identity.
	 */
	public void setUsernameField(String usernameField) {
		this.usernameField = usernameField;
	}

	/**
	 * @param passwordField
	 *            The field holding the password to authenticate the server to
	 *            the SMS gateway.
	 */
	public void setPasswordField(String passwordField) {
		this.passwordField = passwordField;
	}

	/**
	 * @param destinationField
	 *            The field holding the number to send the SMS to.
	 */
	public void setDestinationField(String destinationField) {
		this.destinationField = destinationField;
	}

	/**
	 * @param messageField
	 *            The field holding the plain-text message to send.
	 */
	public void setMessageField(String messageField) {
		this.messageField = messageField;
	}

	@PostConstruct
	void init() {
		client = new DefaultHttpClient();
	}

	@PreDestroy
	void close() {
		client = null;
	}

	@Override
	public void reconfigured() {
		String s = getParam("service");
		try {
			if (s.isEmpty()) {
				log.warn("did not get sms.service from servlet config; using default ("
						+ DEFAULT_SMS_GATEWAY + ")");
				s = DEFAULT_SMS_GATEWAY;
			}
			service = URI.create(s);
			user = getParam("user");
			pass = getParam("pass");
		} catch (RuntimeException e) {
			service = null;
			user = pass = "";
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
		List<NameValuePair> params = new ArrayList<NameValuePair>();
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
		if (entity != null) {
			BufferedReader e = new BufferedReader(new InputStreamReader(
					entity.getContent()));
			try {
				log.info(e.readLine());
			} finally {
				e.close();
			}
		}
	}
}
