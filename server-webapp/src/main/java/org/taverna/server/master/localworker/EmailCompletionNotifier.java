/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Transport.send;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.taverna.server.master.TavernaServerImpl.log;

import java.text.MessageFormat;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.springframework.beans.factory.annotation.Required;

/**
 * Completion notifier that sends messages by email.
 * 
 * @author Donal Fellows
 */
public class EmailCompletionNotifier implements CompletionNotifier,
		MessageDispatcher {
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
	 * @param subject
	 *            The subject of the notification email.
	 */
	@Required
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @param messageFormat
	 *            The template for the body of the message to send. Parameter #0
	 *            will be substituted with the ID of the job, and parameter #1
	 *            will be substituted with the exit code.
	 */
	public void setMessageFormat(String messageFormat) {
		this.format = new MessageFormat(messageFormat);
	}

	/**
	 * @param contentType
	 *            The content type of the message to be sent. For example, "
	 *            <tt>text/plain</tt>".
	 */
	public void setMessageContentType(String contentType) {
		this.contentType = contentType;
	}

	public static final String DEFAULT_MESSAGE_FORMAT = "Your job with ID={0} has finished with exit code {1,number,integer}.";
	private InternetAddress from;
	private String subject;
	private MessageFormat format = new MessageFormat(DEFAULT_MESSAGE_FORMAT);
	private String contentType = TEXT_PLAIN;

	@Override
	public String notifyComplete(String name, RemoteRunDelegate run,
			int code) {
		return format.format(new Object[] { name, code });
	}

	@Override
	public String getTargetDispatcher() {
		return "finishedEmail";
	}

	@Override
	public void dispatch(String messageContent, String targetParameter)
			throws Exception {
		// Simple checks for acceptability
		String to = targetParameter.trim();
		if (to.startsWith("mailto:"))
			to = to.substring(7);
		if (!to.matches(".+@.+")) {
			log.info("did not send email notification: improper email address \""
					+ to + "\"");
			return;
		}

		Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
		Session session = (Session) envCtx.lookup("mail/Session");

		if (session == null)
			return;
		Message message = new MimeMessage(session);
		message.setFrom(from);
		message.setRecipient(TO, new InternetAddress(to.trim()));
		message.setSubject(subject);
		message.setContent(messageContent, contentType);
		send(message);
	}
}