/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.usage;

import static org.taverna.server.master.TavernaServerImpl.log;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;

import org.ogf.usage.JobUsageRecord;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ManagementModel;
import org.taverna.server.master.utils.Contextualizer;
import org.taverna.server.master.utils.JDOSupport;

/**
 * A simple state-aware writer of usage records. It just appends them, one by
 * one, to the file whose name is stored in the state.
 * 
 * @author Donal Fellows
 */
public class UsageRecordRecorder extends JDOSupport<UsageRecord> {
	public UsageRecordRecorder() {
		super(UsageRecord.class);
	}

	private String logFile = null;
	private boolean disableDB = false;
	private ManagementModel state;
	private Contextualizer contextualizer;
	private String logDestination;
	private PrintWriter writer;
	private Object lock = new Object();
	private UsageRecordRecorder self;

	/**
	 * @param state
	 *            the state to set
	 */
	@Required
	public void setState(ManagementModel state) {
		this.state = state;
	}

	@Required
	public void setSelf(UsageRecordRecorder self) {
		this.self = self;
	}

	public void setLogFile(String logFile) {
		this.logFile = (logFile == null || logFile.equals("none")) ? null : logFile;
	}

	public void setDisableDB(String disable) {
		disableDB = "yes".equalsIgnoreCase(disable);
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

	/**
	 * Accept a usage record for recording.
	 * 
	 * @param usageRecord
	 *            The serialized usage record to record.
	 */
	public void storeUsageRecord(String usageRecord) {
		String logfile = state.getUsageRecordLogFile();
		if (logfile == null) {
			logfile = logFile;
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

		if (!disableDB)
			saveURtoDB(usageRecord);
	}

	/**
	 * How to save a usage record to the database.
	 * 
	 * @param usageRecord
	 *            The serialized usage record to save.
	 */
	protected void saveURtoDB(String usageRecord) {
		UsageRecord ur;
		try {
			ur = new UsageRecord(usageRecord);
		} catch (JAXBException e) {
			log.warn("failed to deserialize usage record", e);
			return;
		}

		try {
			self.saveURtoDB(ur);
		} catch (RuntimeException e) {
			log.warn("failed to save UR to database", e);
		}
	}

	@WithinSingleTransaction
	public void saveURtoDB(UsageRecord ur) {
		persist(ur);
	}

	@WithinSingleTransaction
	public List<JobUsageRecord> getUsageRecords() {
		@SuppressWarnings("unchecked")
		Collection<String> urs = (Collection<String>) namedQuery("allByDate")
				.execute();
		List<JobUsageRecord> result = new ArrayList<JobUsageRecord>();
		for (String ur : urs)
			try {
				result.add(JobUsageRecord.unmarshal(ur));
			} catch (JAXBException e) {
				log.warn("failed to unmarshal UR", e);
			}
		return result;
	}

	@Override
	@PreDestroy
	public void close() {
		super.close();
		if (writer != null)
			writer.close();
	}
}