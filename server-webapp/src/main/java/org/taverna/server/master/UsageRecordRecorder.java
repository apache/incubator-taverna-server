/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.utils.Contextualizer;

/**
 * A simple state-aware writer of usage records. It just appends them, one by
 * one, to the file whose name is stored in the state.
 * 
 * @author Donal Fellows
 */
public class UsageRecordRecorder implements ServletContextAware {
	private ManagementModel state;
	private Contextualizer contextualizer;
	private ServletContext servlet;

	/**
	 * @param state
	 *            the state to set
	 */
	@Required
	public void setState(ManagementModel state) {
		this.state = state;
	}

	/**
	 * @param contextualizer
	 *            the system's contextualizer, used to allow making the UR dump
	 *            file be placed relative to the webapp.
	 */
	@Required
	public void setContextualizer(Contextualizer contextualizer) {
		this.contextualizer = contextualizer;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servlet = servletContext;
	}

	private String logDestination;
	private PrintWriter writer;

	public synchronized void storeUsageRecord(String usageRecord)
			throws IOException {
		String logfile = state.getUsageRecordLogFile();
		if (logfile == null && servlet != null) {
			// TODO: Document the usageRecordLogFile parameter
			logfile = servlet.getInitParameter("usageRecordLogFile");
		}
		if (logfile == null)
			return;
		logfile = contextualizer.contextualize(logfile);
		if (!logfile.equals(logDestination)) {
			if (writer != null) {
				writer.close();
				writer = null;
			}
			writer = new PrintWriter(new FileWriter(logfile));
			logDestination = logfile;
		}
		writer.println(usageRecord);
		writer.flush();
	}

	@PreDestroy
	public void close() {
		if (writer != null)
			writer.close();
	}
}
