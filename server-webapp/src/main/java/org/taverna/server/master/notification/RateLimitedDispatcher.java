/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.taverna.server.master.interfaces.MessageDispatcher;

/**
 * Rate-limiting support. Some message fabrics simply should not be used to send
 * a lot of messages.
 * 
 * @author Donal Fellows
 */
public abstract class RateLimitedDispatcher implements MessageDispatcher {
	private int cooldownSeconds;
	private Map<String, DateTime> lastSend = new HashMap<String, DateTime>();

	/**
	 * Set how long must elapse between updates to the status of any particular
	 * user. Calls before that time are just silently dropped.
	 * 
	 * @param cooldownSeconds
	 *            Time to elapse, in seconds.
	 */
	public void setCooldownSeconds(int cooldownSeconds) {
		this.cooldownSeconds = cooldownSeconds;
	}

	protected boolean isSendAllowed(String who) {
		DateTime now = new DateTime();
		synchronized (lastSend) {
			DateTime last = lastSend.get(who);
			if (last != null) {
				if (!now.isAfter(last.plusSeconds(cooldownSeconds)))
					return false;
			}
			lastSend.put(who, now);
		}
		return true;
	}
}
