/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */

package org.taverna.server.master.notification;

import static org.taverna.server.master.TavernaServerImpl.log;

import javax.annotation.PreDestroy;
import javax.servlet.ServletConfig;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.springframework.web.context.ServletConfigAware;
import org.taverna.server.master.interfaces.MessageDispatcher;

/**
 * Send notifications by Jabber/XMPP.
 * 
 * @author Donal Fellows
 */
public class JabberDispatcher implements MessageDispatcher, ServletConfigAware {
	private XMPPConnection conn;

	public static final String SERVICE_PROPERTY_NAME = "xmpp.service";
	public static final String USER_PROPERTY_NAME = "xmpp.user";
	public static final String PASSWORD_PROPERTY_NAME = "xmpp.password";

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		close();
		try {
			String host = servletConfig.getInitParameter(SERVICE_PROPERTY_NAME);
			if (host == null) {
				log.debug("no " + SERVICE_PROPERTY_NAME
						+ " property in servlet config; disabling XMPP support");
				return;
			}
			String user = servletConfig.getInitParameter(USER_PROPERTY_NAME);
			if (user == null) {
				log.debug("no " + USER_PROPERTY_NAME
						+ " property in servlet config; disabling XMPP support");
				return;
			}
			String pass = servletConfig
					.getInitParameter(PASSWORD_PROPERTY_NAME);
			if (pass == null) {
				log.debug("no " + PASSWORD_PROPERTY_NAME
						+ " property in servlet config; disabling XMPP support");
				return;
			}
			ConnectionConfiguration cfg = new ConnectionConfiguration(host);
			cfg.setSendPresence(false);
			XMPPConnection c = new XMPPConnection(cfg);
			c.connect();
			c.login(user, pass, "TavernaServer");
			conn = c;
			log.info("connected to XMPP service <" + host + "> as user <"
					+ user + ">");
		} catch (Exception e) {
			log.info("failed to connect to XMPP server", e);
		}
	}

	@PreDestroy
	public void close() {
		if (conn != null)
			conn.disconnect();
		conn = null;
	}

	@Override
	public boolean isAvailable() {
		return conn != null;
	}

	@Override
	public void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception {
		Chat chat = conn.getChatManager().createChat(targetParameter,
				new DroppingListener());
		Message m = new Message();
		m.addBody(null, messageContent);
		m.setSubject(messageSubject);
		chat.sendMessage(m);
	}

	static class DroppingListener implements MessageListener {
		@Override
		public void processMessage(Chat chat, Message message) {
			log.debug("unexpectedly received XMPP message from <"
					+ message.getFrom() + ">; ignoring");
		}
	}
}
