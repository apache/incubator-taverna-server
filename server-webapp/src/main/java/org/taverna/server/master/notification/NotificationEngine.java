/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.interfaces.MessageDispatcher;

/**
 * A common object for handling dispatch of event-driven messages.
 * 
 * @author Donal Fellows
 */
public class NotificationEngine {
	static Log log = LogFactory.getLog("Taverna.Server.Notification");

	@PreDestroy
	void closeLog() {
		log = null;
	}

	private Map<String, MessageDispatcher> dispatchers;

	/**
	 * @param dispatcherMap
	 *            A mapping from names to dispatch objects.
	 */
	@Required
	public void setDispatchers(Map<String, MessageDispatcher> dispatcherMap) {
		dispatchers = dispatcherMap;
	}

	/**
	 * Dispatch a message over the notification fabric.
	 * 
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
	public void dispatchMessage(String destination, String subject,
			String message) throws Exception {
		try {
			URI toURI = new URI(destination.trim());
			MessageDispatcher d = dispatchers.get(toURI.getScheme());
			if (d != null && d.isAvailable())
				d.dispatch(subject, message, toURI.getSchemeSpecificPart());
			else
				log.warn("no such notification dispatcher for "
						+ toURI.getScheme());
		} catch (URISyntaxException e) {
			// See if *someone* will handle the message
			Exception e2 = null;
			for (MessageDispatcher d : dispatchers.values())
				try {
					if (d.isAvailable()) {
						d.dispatch(subject, message, destination);
						return;
					}
				} catch (Exception ex) {
					log.debug("failed in pseudo-directed dispatch of "
							+ destination, ex);
					e2 = ex;
				}
			if (e2 != null)
				throw e2;
		}
	}

	/**
	 * What message dispatchers are actually available (i.e., not disabled by
	 * configuration somewhere).
	 */
	public List<String> listAvailableDispatchers() {
		ArrayList<String> result = new ArrayList<String>();
		for (Map.Entry<String, MessageDispatcher> entry : dispatchers
				.entrySet()) {
			if (entry.getValue().isAvailable())
				result.add(entry.getKey());
		}
		return result;
	}
}
