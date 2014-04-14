/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.notification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.interfaces.MessageDispatcher;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * A common object for handling dispatch of event-driven messages.
 * 
 * @author Donal Fellows
 */
public class NotificationEngine {
	private Log log = LogFactory.getLog("Taverna.Server.Notification");
	private Map<String, MessageDispatcher> dispatchers;
	private List<MessageDispatcher> universalDispatchers;

	/**
	 * @param dispatchers
	 *            The various dispatchers we want to install.
	 */
	@Required
	public void setDispatchers(List<MessageDispatcher> dispatchers) {
		this.dispatchers = new HashMap<>();
		for (MessageDispatcher d : dispatchers)
			this.dispatchers.put(d.getName(), d);
	}

	/**
	 * @param dispatcherList
	 *            A list of dispatch objects to always dispatch to.
	 */
	@Required
	public void setUniversalDispatchers(List<MessageDispatcher> dispatcherList) {
		this.universalDispatchers = dispatcherList;
	}

	private void dispatchToChosenTarget(TavernaRun originator, String scheme,
			String target, Message message) throws Exception {
		try {
			MessageDispatcher d = dispatchers.get(scheme);
			if (d != null && d.isAvailable())
				d.dispatch(originator, message.getTitle(scheme),
						message.getContent(scheme), target);
			else
				log.warn("no such notification dispatcher for " + scheme);
		} catch (URISyntaxException e) {
			// See if *someone* will handle the message
			Exception e2 = null;
			for (MessageDispatcher d : dispatchers.values())
				try {
					if (d.isAvailable()) {
						d.dispatch(originator, message.getTitle(d.getName()),
								message.getContent(d.getName()), scheme + ":"
										+ target);
						return;
					}
				} catch (Exception ex) {
					if (log.isDebugEnabled())
						log.debug("failed in pseudo-directed dispatch of "
								+ scheme + ":" + target, ex);
					e2 = ex;
				}
			if (e2 != null)
				throw e2;
		}
	}

	private void dispatchUniversally(TavernaRun originator, Message message)
			throws Exception {
		for (MessageDispatcher d : universalDispatchers)
			try {
				if (d.isAvailable())
					d.dispatch(originator, message.getTitle(d.getName()),
							message.getContent(d.getName()), null);
			} catch (Exception e) {
				log.warn("problem in universal dispatcher", e);
			}
	}

	/**
	 * Dispatch a message over the notification fabric.
	 * 
	 * @param originator
	 *            What workflow run was the source of this message?
	 * @param destination
	 *            Where the message should get delivered to. The correct format
	 *            of this is either as a URI of some form (where the scheme
	 *            determines the dispatcher) or as an invalid URI in which case
	 *            it is just tried against the possibilities to see if any
	 *            succeeds.
	 * @param subject
	 *            The subject line of the message.
	 * @param message
	 *            The plain text body of the message.
	 * @throws Exception
	 *             If anything goes wrong with the dispatch process.
	 */
	public void dispatchMessage(TavernaRun originator, String destination,
			Message message) throws Exception {
		if (destination != null && !destination.trim().isEmpty()) {
			try {
				URI toURI = new URI(destination.trim());
				dispatchToChosenTarget(originator, toURI.getScheme(),
						toURI.getSchemeSpecificPart(), message);
			} catch (URISyntaxException e) {
				// Ignore
			}
		}
		dispatchUniversally(originator, message);
	}

	/**
	 * @return The message dispatchers that are actually available (i.e., not
	 *         disabled by configuration somewhere).
	 */
	public List<String> listAvailableDispatchers() {
		ArrayList<String> result = new ArrayList<>();
		for (Map.Entry<String, MessageDispatcher> entry : dispatchers
				.entrySet()) {
			if (entry.getValue().isAvailable())
				result.add(entry.getKey());
		}
		return result;
	}

	public interface Message {
		String getContent(String type);

		String getTitle(String type);
	}
}
