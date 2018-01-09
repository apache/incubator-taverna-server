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

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Required;
import org.apache.taverna.server.master.common.version.Version;
import org.apache.taverna.server.master.exceptions.NoListenerException;
import org.apache.taverna.server.master.interfaces.Listener;
import org.apache.taverna.server.master.interfaces.UriBuilderFactory;

public class VelocityCompletionNotifier implements CompletionNotifier {
	private String subject;
	private VelocityEngine engine;
	private Template template;
	private String name;
	private String templateName;
	private UriBuilderFactory ubf;

	@Override
	public String getName() {
		return name;
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
	 * @param engine
	 *            The configured Apache Velocity engine.
	 */
	@Required
	public void setVelocityEngine(VelocityEngine engine) {
		this.engine = engine;
	}

	/**
	 * @param uriBuilderFactory
	 *            The configured URI builder factory.
	 */
	@Required
	public void setUriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.ubf = uriBuilderFactory;
	}

	/**
	 * @param name
	 *            The name of the template.
	 */
	@Required
	public void setName(String name) {
		this.name = name;
		this.templateName = getClass().getName() + "_" + name + ".vtmpl";
	}

	private Template getTemplate() {
		if (template == null)
			synchronized(this) {
				if (template == null)
					template = engine.getTemplate(templateName);
			}
		return template;
	}

	@Override
	public String makeCompletionMessage(String name, RemoteRunDelegate run,
			int code) {
		VelocityContext ctxt = new VelocityContext();
		ctxt.put("id", name);
		ctxt.put("uriBuilder", ubf.getRunUriBuilder(run));
		ctxt.put("name", run.getName());
		ctxt.put("creationTime", run.getCreationTimestamp());
		ctxt.put("startTime", run.getStartTimestamp());
		ctxt.put("finishTime", run.getFinishTimestamp());
		ctxt.put("expiryTime", run.getExpiry());
		ctxt.put("serverVersion", Version.JAVA);
		for (Listener l : run.getListeners())
			if (l.getName().equals("io")) {
				for (String p : l.listProperties())
					try {
						ctxt.put("prop_" + p, l.getProperty(p));
					} catch (NoListenerException e) {
						// Ignore...
					}
				break;
			}
		StringWriter sw = new StringWriter();
		getTemplate().merge(ctxt, sw);
		return sw.toString();
	}

	@Override
	public String makeMessageSubject(String name, RemoteRunDelegate run,
			int code) {
		return subject;
	}
}
