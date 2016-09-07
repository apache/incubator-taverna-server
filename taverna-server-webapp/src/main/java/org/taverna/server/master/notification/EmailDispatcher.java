/*
 */
package org.taverna.server.master.notification;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * How to send a plain text message by email to someone.
 * 
 * @author Donal Fellows
 */
public class EmailDispatcher extends RateLimitedDispatcher {
	@Override
	public String getName() {
		return "mailto";
	}

	/**
	 * @param from
	 *            Email address that the notification is to come from.
	 */
	@Required
	public void setFrom(String from) {
		this.from = valid(from, "");
	}

	/**
	 * @param host
	 *            The outgoing SMTP server address.
	 */
	@Required
	public void setSmtpHost(String host) {
		this.host = valid(host, "");
	}

	/**
	 * @param contentType
	 *            The content type of the message to be sent. For example, "
	 *            <tt>text/plain</tt>".
	 */
	public void setMessageContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * @param sender
	 *            the sender to set
	 */
	public void setSender(MailSender sender) {
		this.sender = sender;
	}

	private String from;
	private String host;
	private MailSender sender;
	@SuppressWarnings("unused")
	private String contentType = TEXT_PLAIN;

	/**
	 * Try to perform the lookup of the email service. This is called during
	 * configuration so that any failure happens at a useful, predictable time.
	 */
	@PostConstruct
	public void tryLookup() {
		if (!isAvailable()) {
			log.warn("no mail support; disabling email dispatch");
			sender = null;
			return;
		}
		try {
			if (sender instanceof JavaMailSender)
				((JavaMailSender) sender).createMimeMessage();
		} catch (Throwable t) {
			log.warn("sender having problems constructing messages; "
					+ "disabling...", t);
			sender = null;
		}
	}

	@Override
	public void dispatch(String messageSubject, String messageContent, String to)
			throws Exception {
		// Simple checks for acceptability
		if (!to.matches(".+@.+")) {
			log.info("did not send email notification: improper email address \""
					+ to + "\"");
			return;
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to.trim());
		message.setSubject(messageSubject);
		message.setText(messageContent);
		sender.send(message);
	}

	@Override
	public boolean isAvailable() {
		return (host != null && !host.isEmpty() && sender != null
				&& from != null && !from.isEmpty());
	}
}
