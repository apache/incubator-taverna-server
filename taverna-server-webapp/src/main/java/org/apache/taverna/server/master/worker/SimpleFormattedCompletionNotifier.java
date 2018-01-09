/*
 */
package org.apache.taverna.server.master.worker;
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

import static org.apache.taverna.server.master.defaults.Default.NOTIFY_MESSAGE_FORMAT;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Required;

/**
 * Completion notifier that sends messages by email.
 * 
 * @author Donal Fellows
 */
public class SimpleFormattedCompletionNotifier implements CompletionNotifier {
	@Required
	public void setName(String name) {
		this.name = name;
	}

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

	private String name;
	private String subject;
	private MessageFormat format = new MessageFormat(NOTIFY_MESSAGE_FORMAT);

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

	@Override
	public String getName() {
		return name;
	}
}