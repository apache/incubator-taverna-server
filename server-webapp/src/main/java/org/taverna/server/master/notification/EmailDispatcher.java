/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.taverna.server.master.notification.NotificationEngine.log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
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
	 */
	@Required
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @param contentType
	 *            The content type of the message to be sent. For example, "
	 *            <tt>text/plain</tt>".
	 */
	public void setMessageContentType(String contentType) {
		this.contentType = contentType;
	}

	private String from;
	private String contentType = TEXT_PLAIN;

	private Object recipientTo;
	private Constructor<?> makeMessage, makeAddress;
	private Method setFrom, setRecipient, setSubject, setContent, send;
	private Class<?> session;

	/**
	 * Load the mail API from the given class loader.
	 */
	private void initAPI(ClassLoader cl) throws ClassNotFoundException,
			NoSuchMethodException, NoSuchFieldException {
		/*
		 * OMG! This is nasty! Don't know who will be providing the Java Mail
		 * API (there are potentially multiple providers!) so we must soft-code
		 * the whole use of the API so it uses the class loader that provided
		 * the entry point. This makes the code more than a little demented...
		 */
		Class<?> string, message, address, transport, recipient;

		string = String.class;
		session = cl.loadClass("javax.mail.Session");
		message = cl.loadClass("javax.mail.internet.MimeMessage");
		address = cl.loadClass("javax.mail.internet.InternetAddress");
		recipient = cl.loadClass("javax.mail.Message.RecipientType");
		transport = cl.loadClass("javax.mail.Transport");

		makeMessage = message.getConstructor(session);
		makeAddress = address.getConstructor(string);
		setFrom = message.getMethod("setFrom", address);
		setRecipient = message.getMethod("setRecipient", recipient, address);
		setSubject = message.getMethod("setSubject", string);
		setContent = message.getMethod("setContent", string, string);
		send = transport.getMethod("send", message);
		recipientTo = recipient.getField("TO");
	}

	private Object mail() throws NamingException, NoSuchFieldException,
			ClassNotFoundException, NoSuchMethodException {
		Context env = (Context) new InitialContext().lookup("java:comp/env");
		try {
			Object o = env.lookup("mail/Session");
			if (o == null) {
				log.info("no mail/Sesssion in JNDI");
				return null;
			}
			if (recipientTo == null) {
				initAPI(o.getClass().getClassLoader());
				assert recipientTo != null;
			}
			if (session.isInstance(o))
				return o;
			throw new NamingException("unexpected type?! " + o.getClass());
		} catch (NameNotFoundException e) {
			log.info("no mail/Sesssion in JNDI");
			return null;
		}
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

		Object theSession = mail();

		if (theSession == null)
			return;
		try {
			Object realfrom = makeAddress.newInstance(from);
			Object realto = makeAddress.newInstance(to.trim());
			Object msg = makeMessage.newInstance(theSession);

			setFrom.invoke(msg, realfrom);
			setRecipient.invoke(msg, recipientTo, realto);
			setSubject.invoke(msg, messageSubject);
			setContent.invoke(msg, messageContent, contentType);

			send.invoke(null, msg);
		} catch (InvocationTargetException e) {
			throw (Exception) e.getTargetException();
		}
	}

	@Override
	public boolean isAvailable() {
		if (session == null || recipientTo == null)
			return false;
		try {
			return from != null && null != mail();
		} catch (Exception e) {
			return false;
		}
	}
}
