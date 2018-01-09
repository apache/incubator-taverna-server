/*
 */
package org.apache.taverna.server.master.notification.atom;
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

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.jdo.annotations.PersistenceAware;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Required;
import org.apache.taverna.server.master.interfaces.MessageDispatcher;
import org.apache.taverna.server.master.interfaces.TavernaRun;
import org.apache.taverna.server.master.interfaces.UriBuilderFactory;
import org.apache.taverna.server.master.utils.JDOSupport;
import org.apache.taverna.server.master.utils.UsernamePrincipal;

/**
 * The database interface that supports the event feed.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
public class EventDAO extends JDOSupport<Event> implements MessageDispatcher {
	public EventDAO() {
		super(Event.class);
	}

	@Override
	public String getName() {
		return "atom";
	}

	private Log log = LogFactory.getLog("Taverna.Server.Atom");
	private UriBuilderFactory ubf;
	private int expiryAgeDays;

	@Required
	public void setExpiryAgeDays(int expiryAgeDays) {
		this.expiryAgeDays = expiryAgeDays;
	}

	@Required
	public void setUriBuilderFactory(UriBuilderFactory ubf) {
		this.ubf = ubf;
	}

	/**
	 * Get the given user's list of events.
	 * 
	 * @param user
	 *            The identity of the user to get the events for.
	 * @return A copy of the list of events currently known about.
	 */
	@Nonnull
	@WithinSingleTransaction
	public List<Event> getEvents(@Nonnull UsernamePrincipal user) {
		List<String> ids = eventsForUser(user);
		if (log.isDebugEnabled())
			log.debug("found " + ids.size() + " events for user " + user);

		List<Event> result = new ArrayList<>();
		for (String id : ids) {
			Event event = getById(id);
			result.add(detach(event));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<String> eventsForUser(UsernamePrincipal user) {
		return (List<String>) namedQuery("eventsForUser").execute(
				user.getName());
	}

	/**
	 * Get a particular event.
	 * 
	 * @param user
	 *            The identity of the user to get the event for.
	 * @param id
	 *            The handle of the event to look up.
	 * @return A copy of the event.
	 */
	@Nonnull
	@WithinSingleTransaction
	public Event getEvent(@Nonnull UsernamePrincipal user, @Nonnull String id) {
		List<String> ids = eventsForUserAndId(user, id);
		if (log.isDebugEnabled())
			log.debug("found " + ids.size() + " events for user " + user
					+ " with id = " + id);

		if (ids.size() != 1)
			throw new IllegalArgumentException("no such id");
		return detach(getById(ids.get(0)));
	}

	@SuppressWarnings("unchecked")
	private List<String> eventsForUserAndId(UsernamePrincipal user, String id) {
		return (List<String>) namedQuery("eventForUserAndId").execute(
				user.getName(), id);
	}

	/**
	 * Delete a particular event.
	 * 
	 * @param id
	 *            The identifier of the event to delete.
	 */
	@WithinSingleTransaction
	public void deleteEventById(@Nonnull String id) {
		delete(getById(id));
	}

	/**
	 * Delete all events that have expired.
	 */
	@WithinSingleTransaction
	public void deleteExpiredEvents() {
		Date death = new DateTime().plusDays(-expiryAgeDays).toDate();
		death = new Timestamp(death.getTime()); // UGLY SQL HACK

		List<String> ids = eventsFromBefore(death);
		if (log.isDebugEnabled() && !ids.isEmpty())
			log.debug("found " + ids.size()
					+ " events to be squelched (older than " + death + ")");

		for (String id : ids)
			delete(getById(id));
	}

	@SuppressWarnings("unchecked")
	private List<String> eventsFromBefore(Date death) {
		return (List<String>) namedQuery("eventsFromBefore").execute(death);
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	private BlockingQueue<Event> insertQueue = new ArrayBlockingQueue<>(16);

	@Override
	public void dispatch(TavernaRun originator, String messageSubject,
			String messageContent, String targetParameter) throws Exception {
		insertQueue.put(new Event("finish", ubf.getRunUriBuilder(originator)
				.build(), originator.getSecurityContext().getOwner(),
				messageSubject, messageContent));
	}

	public void started(TavernaRun originator, String messageSubject,
			String messageContent) throws InterruptedException {
		insertQueue.put(new Event("start", ubf.getRunUriBuilder(originator)
				.build(), originator.getSecurityContext().getOwner(),
				messageSubject, messageContent));
	}

	private Thread eventDaemon;
	private boolean shuttingDown = false;

	@Required
	public void setSelf(final EventDAO dao) {
		eventDaemon = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (!shuttingDown && !interrupted()) {
						transferEvents(dao, new ArrayList<Event>(
								asList(insertQueue.take())));
						sleep(5000);
					}
				} catch (InterruptedException e) {
				} finally {
					transferEvents(dao, new ArrayList<Event>());
				}
			}
		}, "ATOM event daemon");
		eventDaemon.setContextClassLoader(null);
		eventDaemon.setDaemon(true);
		eventDaemon.start();
	}

	private void transferEvents(EventDAO dao, List<Event> e) {
		insertQueue.drainTo(e);
		dao.storeEvents(e);
	}

	@PreDestroy
	void stopDaemon() {
		shuttingDown = true;
		if (eventDaemon != null)
			eventDaemon.interrupt();
	}

	@WithinSingleTransaction
	protected void storeEvents(List<Event> events) {
		for (Event e : events)
			persist(e);
		log.info("stored " + events.size() + " notification events");
	}
}
