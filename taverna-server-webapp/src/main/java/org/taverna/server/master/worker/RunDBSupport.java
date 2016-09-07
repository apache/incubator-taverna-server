/*
 */
package org.taverna.server.master.worker;
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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.taverna.server.master.notification.NotificationEngine;

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
	void flushToDisk(@Nonnull RemoteRunDelegate run);

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
	@Nonnull
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