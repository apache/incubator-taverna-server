/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Transport.send;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.taverna.server.master.notification.NotificationEngine.log;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.interfaces.MessageDispatcher;

/**
 * How to send a plain text message by email to someone.
 * 
 * @author Donal Fellows
 */
public class EmailDispatcher implements MessageDispatcher {
	/**
	 * @param from
	 *            Email address that the notification is to come from.
	 * @throws AddressException
	 *             If a bad email address is given.
	 */
	@Required
	public void setFrom(String from) throws AddressException {
		this.from = new InternetAddress(from);
	}

	/**
	 * @param contentType
	 *            The content type of the message to be sent. For example, "
	 *            <tt>text/plain</tt>".
	 */
	public void setMessageContentType(String contentType) {
		this.contentType = contentType;
	}

	private InternetAddress from;
	private String contentType = TEXT_PLAIN;

	private Context env() throws NamingException {
		return (Context) new InitialContext().lookup("java:comp/env");
	}

	private Session mail() throws NamingException {
		return (Session) env().lookup("mail/Session");
	}

	@PostConstruct
	public void tryLookup() {
		try {
			if (mail() == null)
				log.warn("failed to look up mail library in JNDI; "
						+ "disabling email dispatch");
		} catch (Exception e) {
			log.warn("failed to look up mail library in JNDI; "
					+ "disabling email dispatch", e);
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

		Session session = mail();

		if (session == null)
			return;
		Message message = new MimeMessage(session);
		message.setFrom(from);
		message.setRecipient(TO, new InternetAddress(to.trim()));
		message.setSubject(messageSubject);
		message.setContent(messageContent, contentType);
		send(message);
	}

	@Override
	public boolean isAvailable() {
		try {
			return from != null && null != mail();
		} catch (Exception e) {
			return false;
		}
	}
}
