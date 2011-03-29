/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.usage;

import static org.taverna.server.master.TavernaServerImpl.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PreDestroy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.ManagementModel;
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
	private ServletContext config;
	private PersistenceManager persistenceManager;
	private String logDestination;
	private PrintWriter writer;
	private Object lock = new Object();

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		persistenceManager = persistenceManagerFactory.getPersistenceManager();
	}

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
	public void setServletContext(ServletContext servletConfig) {
		this.config = servletConfig;
	}

	/**
	 * Accept a usage record for recording.
	 * 
	 * @param usageRecord
	 *            The serialized usage record to record.
	 * @throws IOException
	 *             If the recording goes wrong.
	 */
	public void storeUsageRecord(String usageRecord) throws IOException {
		String logfile = state.getUsageRecordLogFile();
		if (logfile == null && config != null) {
			logfile = config.getInitParameter("usage.logFile");
		}
		if (logfile != null) {
			logfile = contextualizer.contextualize(logfile);
			synchronized (lock) {
				if (!logfile.equals(logDestination)) {
					if (writer != null) {
						writer.close();
						writer = null;
					}
					try {
						writer = new PrintWriter(new FileWriter(logfile));
						logDestination = logfile;
					} catch (IOException e) {
						log.warn("failed to open usage record log file", e);
					}
				}
				if (writer != null) {
					writer.println(usageRecord);
					writer.flush();
				}
			}
		}

		if ("yes".equals(config.getInitParameter("usage.disableDB"))
				&& persistenceManager != null)
			saveURtoDB(usageRecord);
	}

	/**
	 * How to save a usage record to the database.
	 * 
	 * @param usageRecord
	 *            The serialized usage record to save.
	 */
	protected void saveURtoDB(String usageRecord) {
		try {
			UsageRecord ur = new UsageRecord(usageRecord);
			Transaction tx = persistenceManager.currentTransaction();
			if (tx.isActive())
				tx = null;
			else
				tx.begin();
			try {
				persistenceManager.makePersistent(ur);
			} catch (RuntimeException e) {
				if (tx != null)
					tx.rollback();
				throw e;
			} finally {
				if (tx != null)
					tx.commit();
			}
		} catch (JAXBException e) {
			log.warn("failed to deserialize usage record", e);
		}
	}

	@PreDestroy
	public void close() {
		if (writer != null)
			writer.close();
		if (persistenceManager != null)
			persistenceManager.close();
	}
}