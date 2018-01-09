/*
 */
package org.taverna.server.master.notification;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.taverna.server.master.interfaces.MessageDispatcher;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * Rate-limiting support. Some message fabrics simply should not be used to send
 * a lot of messages.
 * 
 * @author Donal Fellows
 */
public abstract class RateLimitedDispatcher implements MessageDispatcher {
	/** Pre-configured logger. */
	protected Log log = LogFactory.getLog("Taverna.Server.Notification");
	private int cooldownSeconds;
	private Map<String, DateTime> lastSend = new HashMap<>();

	String valid(String value, String def) {
		if (value == null || value.trim().isEmpty()
				|| value.trim().startsWith("${"))
			return def;
		else
			return value.trim();
	}

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

	/**
	 * Test whether the rate limiter allows the given user to send a message.
	 * 
	 * @param who
	 *            Who wants to send the message?
	 * @return <tt>true</tt> iff they are permitted.
	 */
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

	@Override
	public void dispatch(TavernaRun ignored, String messageSubject,
			String messageContent, String target) throws Exception {
		if (isSendAllowed(target))
			dispatch(messageSubject, messageContent, target);
	}

	/**
	 * Dispatch a message to a recipient that doesn't care what produced it.
	 * 
	 * @param messageSubject
	 *            The subject of the message to send.
	 * @param messageContent
	 *            The plain-text content of the message to send.
	 * @param target
	 *            A description of where it is to go.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public abstract void dispatch(String messageSubject, String messageContent,
			String target) throws Exception;
}
