package org.taverna.server.master.worker;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.interfaces.Listener;

public class VelocityCompletionNotifier implements CompletionNotifier {
	private String subject;
	private VelocityEngine engine;
	private Template template;
	private String templateName;

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
	 * @param templateName
	 *            The name of the template.
	 */
	@Required
	public void setTemplate(String templateName) {
		this.templateName = templateName;
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
		ctxt.put("name", run.getName());
		ctxt.put("creationTime", run.getCreationTimestamp());
		ctxt.put("startTime", run.getStartTimestamp());
		ctxt.put("finishTime", run.getFinishTimestamp());
		ctxt.put("expiryTime", run.getExpiry());
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
