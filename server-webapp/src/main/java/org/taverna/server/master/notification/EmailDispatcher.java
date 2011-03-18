/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.taverna.server.master.notification.NotificationEngine.log;

import java.lang.reflect.InvocationTargetException;

import javax.annotation.PostConstruct;
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
	 */
	@Required
	public void setFrom(String from) {
		try {
			if (ADDRESS_CLS != null)
			this.from = ADDRESS_CLS.getConstructor(String.class).newInstance(from);
		} catch (InvocationTargetException e) {
			log.warn("failed to construct address", e.getTargetException());
		} catch (Exception e) {
			log.warn("failed to make an instance of " + ADDRESS_CLS.getName(), e);
		}
	}

	/**
	 * @param contentType
	 *            The content type of the message to be sent. For example, "
	 *            <tt>text/plain</tt>".
	 */
	public void setMessageContentType(String contentType) {
		this.contentType = contentType;
	}

	private Object from;
	private String contentType = TEXT_PLAIN;
	private static Class<?> SESSION_CLS, MESSAGE_CLS, ADDRESS_CLS, RECIP_CLS,
			TRANSPORT_CLS;
	private static Object TO_HDR;
	static {
		try {
			SESSION_CLS = Class.forName("javax.mail.Session");
			MESSAGE_CLS = Class.forName("javax.mail.internet.MimeMessage");
			ADDRESS_CLS = Class.forName("javax.mail.internet.InternetAddress");
			RECIP_CLS = Class.forName("javax.mail.Message.RecipientType");
			TRANSPORT_CLS = Class.forName("javax.mail.Transport");
			TO_HDR = RECIP_CLS.getField("TO");
		} catch (Exception e) {
			NotificationEngine.log.fatal("failed to look up mail classes", e);
		}
	}

	private Object mail() throws NamingException {
		Context env = (Context) new InitialContext().lookup("java:comp/env");
		Object o = env.lookup("mail/Session");
		if (SESSION_CLS.isInstance(o))
			return o;
		throw new NamingException("unexpected type?! " + o.getClass());
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

		Object session = mail();

		if (session == null)
			return;
		Object m = MESSAGE_CLS.getConstructor(SESSION_CLS).newInstance(session);
		MESSAGE_CLS.getMethod("setFrom", ADDRESS_CLS).invoke(m, from);
		Object realto = ADDRESS_CLS.getConstructor(String.class).newInstance(
				to.trim());
		MESSAGE_CLS.getMethod("setRecipient", RECIP_CLS, ADDRESS_CLS).invoke(m,
				TO_HDR, realto);
		MESSAGE_CLS.getMethod("setSubject", String.class).invoke(m,
				messageSubject);
		MESSAGE_CLS.getMethod("setContent", String.class, String.class).invoke(
				m, messageContent, contentType);
		TRANSPORT_CLS.getMethod("send", MESSAGE_CLS).invoke(null, m);
	}

	@Override
	public boolean isAvailable() {
		if (SESSION_CLS == null || MESSAGE_CLS == null || ADDRESS_CLS == null
				|| RECIP_CLS == null || TRANSPORT_CLS == null || TO_HDR == null)
			return false;
		try {
			return from != null && null != mail();
		} catch (Exception e) {
			return false;
		}
	}
}
