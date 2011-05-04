/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Required;

/**
 * Completion notifier that sends messages by email.
 * 
 * @author Donal Fellows
 */
public class SimpleFormattedCompletionNotifier implements CompletionNotifier {
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
	 * The default message format to use if none is configured.
	 */
	public static final String DEFAULT_MESSAGE_FORMAT = "Your job with ID={0} has finished with exit code {1,number,integer}.";
	private String subject;
	private MessageFormat format = new MessageFormat(DEFAULT_MESSAGE_FORMAT);

	@Override
	public String makeCompletionMessage(String name, RemoteRunDelegate run,
			int code) {
		return format.format(new Object[] { name, code });
	}

	@Override
	public String makeMessageSubject(String name, RemoteRunDelegate run,
			int code) {
		return subject;
	}
}