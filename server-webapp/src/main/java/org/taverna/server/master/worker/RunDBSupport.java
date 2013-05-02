/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import java.util.List;

import org.taverna.server.master.notification.NotificationEngine;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The interface to the database of runs.
 * 
 * @author Donal Fellows
 */
public interface RunDBSupport {
	/**
	 * Scan each run to see if it has finished yet and issue registered
	 * notifications if it has.
	 */
	void checkForFinishNow();

	/**
	 * Remove currently-expired runs from this database.
	 */
	void cleanNow();

	/**
	 * How many runs are stored in the database.
	 * 
	 * @return The current size of the run table.
	 */
	int countRuns();

	/**
	 * Ensure that a run gets persisted in the database. It is assumed that the
	 * value is already in there.
	 * 
	 * @param run
	 *            The run to persist.
	 */
	void flushToDisk(@NonNull RemoteRunDelegate run);

	/**
	 * Select an arbitrary representative run.
	 * 
	 * @return The selected run.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	@Nullable
	RemoteRunDelegate pickArbitraryRun() throws Exception;

	/**
	 * Get a list of all the run names.
	 * 
	 * @return The names (i.e., UUIDs) of all the runs.
	 */
	@NonNull
	List<String> listRunNames();

	/**
	 * @param notificationEngine
	 *            A reference to the notification fabric bean.
	 */
	void setNotificationEngine(NotificationEngine notificationEngine);

	/**
	 * @param notifier
	 *            A reference to the bean that creates messages about workflow
	 *            run termination.
	 */
	void setNotifier(CompletionNotifier notifier);

	/**
	 * @return A reference to the actual factory for remote runs.
	 */
	FactoryBean getFactory();
}