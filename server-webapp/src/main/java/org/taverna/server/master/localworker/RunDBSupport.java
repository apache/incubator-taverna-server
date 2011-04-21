package org.taverna.server.master.localworker;

import java.util.List;

import org.taverna.server.master.notification.NotificationEngine;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

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

	/** How many runs are stored in the database. */
	int countRuns();

	/**
	 * Ensure that a run gets persisted in the database. It is assumed that the
	 * value is already in there.
	 * 
	 * @param run
	 *            The run to persist.
	 */
	void flushToDisk(@NonNull RemoteRunDelegate run);

	/** Select an arbitrary representative run. */
	@Nullable
	RemoteRunDelegate pickArbitraryRun() throws Exception;

	/** Get a list of all the run names. */
	@NonNull
	List<String> listRunNames();

	void setNotificationEngine(NotificationEngine notificationEngine);

	void setNotifier(CompletionNotifier notifier);
}