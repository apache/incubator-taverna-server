/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */

package org.taverna.server.master.notification;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

/**
 * Send notifications by Jabber/XMPP.
 * 
 * @author Donal Fellows
 */
public class JabberDispatcher extends AbstractConfiguredDispatcher {
	public JabberDispatcher() {
		super("xmpp");
	}

	private XMPPConnection conn;
	private String resource = "TavernaServer";

	/**
	 * @param resource
	 *            The XMPP resource to use when connecting the server. This
	 *            defaults to "<tt>TavernaServer</tt>".
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

	@Override
	public void reconfigured() {
		close();
		try {
			String host = getParam("service");
			String user = getParam("user");
			String pass = getParam("password");
			if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
				log.info("disabling XMPP support; incomplete configuration");
				return;
			}
			ConnectionConfiguration cfg = new ConnectionConfiguration(host);
			cfg.setSendPresence(false);
			XMPPConnection c = new XMPPConnection(cfg);
			c.connect();
			c.login(user, pass, resource);
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
		private Log log = LogFactory.getLog("Taverna.Server.Notification.Jabber");
		@Override
		public void processMessage(Chat chat, Message message) {
			log.debug("unexpectedly received XMPP message from <"
					+ message.getFrom() + ">; ignoring");
		}
	}
}
