/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static net.unto.twitter.Api.builder;
import net.unto.twitter.Api;

import org.taverna.server.master.interfaces.MessageDispatcher;

/**
 * Super simple-minded twitter dispatcher. You need to tell it your username and
 * password as part of the connection parameters, for example via a dispatcher
 * URN of "<tt>twitter:fred:bloggs</tt>" where <tt>fred</tt> is the username and
 * <tt>bloggs</tt> is the password.
 * 
 * @author Donal Fellows
 */
public class TwitterDispatcher implements MessageDispatcher {
	public static final int MAX_MESSAGE_LENGTH = 140;

	private Api getTwitterApi(String user, String pass) {
		return builder().username(user).password(pass).build();
	}

	@Override
	public void dispatch(String messageSubject, String messageContent,
			String targetParameter) throws Exception {
		// messageSubject ignored
		String[] target = targetParameter.split(":", 2);
		if (target == null || target.length != 2)
			throw new Exception("missing username or password");
		Api a = getTwitterApi(target[0], target[1]);

		if (messageContent.length() > MAX_MESSAGE_LENGTH)
			messageContent = messageContent.substring(0, MAX_MESSAGE_LENGTH);
		a.updateStatus(messageContent).build().post();
	}
}
