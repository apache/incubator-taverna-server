package org.taverna.server.master.localworker;

import static javax.mail.Message.RecipientType.TO;
import static javax.mail.Transport.send;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.taverna.server.master.TavernaServerImpl.log;

import javax.mail.Message;
import javax.mail.Session;
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
public class EmailCompletionNotifier implements CompletionNotifier {
	/**
	 * @param from
	 *            Email address that the notification is to come from.
	 */
	@Required
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @param subject
	 *            The subject of the notification email.
	 */
	@Required
	public void setSubject(String subject) {
		this.subject = subject;
	}

	private String from, subject;

	@Override
	public void notifyComplete(RemoteRunDelegate run, String target, int code)
			throws Exception {
		// Simple checks for acceptability
		if (target.startsWith("mailto:"))
			target = target.substring(7);
		if (!target.matches(".+@.+")) {
			log.info("did not send email notification: improper email address \""
					+ target + "\"");
			return;
		}

		Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
		Session session = (Session) envCtx.lookup("mail/Session");

		if (session == null)
			return;
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipient(TO, new InternetAddress(target.trim()));
		message.setSubject(subject);
		message.setContent("Your job " + run + " has finished with exit code "
				+ code, TEXT_PLAIN);
		log.info("about to notify " + target + " about job completion");
		send(message);
	}
}