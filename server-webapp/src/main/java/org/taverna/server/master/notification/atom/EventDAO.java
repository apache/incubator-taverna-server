/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.notification.atom;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.annotations.PersistenceAware;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.interfaces.MessageDispatcher;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.UsernamePrincipal;

import edu.umd.cs.findbugs.annotations.NonNull;

@PersistenceAware
public class EventDAO implements MessageDispatcher {
	private Log log = LogFactory.getLog("Taverna.Server.Atom");
	private PersistenceManager pm;
	private UriBuilderFactory ubf;
	private int expiryAgeDays;

	@Required
	public void setPersistenceManagerFactory(PersistenceManagerFactory pmf) {
		pm = pmf.getPersistenceManagerProxy();
	}

	@Required
	public void setExpiryAgeDays(int expiryAgeDays) {
		this.expiryAgeDays = expiryAgeDays;
	}

	@Required
	public void setUriBuilderFactory(UriBuilderFactory ubf) {
		this.ubf = ubf;
	}

	@NonNull
	public List<AbstractEvent> getEvents(@NonNull UsernamePrincipal user) {
		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			@SuppressWarnings("unchecked")
			List<String> ids = (List<String>) pm.newNamedQuery(
					AbstractEvent.class, "eventsForUser").execute(
					user.getName());
			log.debug("found " + ids.size() + " events for user " + user);
			List<AbstractEvent> result = new ArrayList<AbstractEvent>();
			for (String id : ids) {
				AbstractEvent event = pm.getObjectById(AbstractEvent.class, id);
				result.add(pm.detachCopy(event));
			}
			tx.commit();
			return result;
		} finally {
			if (tx.isActive())
				tx.rollback();
		}
	}

	@NonNull
	public AbstractEvent getEvent(@NonNull UsernamePrincipal user,
			@NonNull String id) {
		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			@SuppressWarnings("unchecked")
			List<String> ids = (List<String>) pm.newNamedQuery(
					AbstractEvent.class, "eventForUserAndId").execute(
					user.getName(), id);
			log.debug("found " + ids.size() + " events for user " + user
					+ " with id = " + id);
			if (ids.size() != 1)
				throw new IllegalArgumentException("no such id");
			AbstractEvent event = pm.detachCopy(pm.getObjectById(
					AbstractEvent.class, ids.get(0)));
			tx.commit();
			return event;
		} finally {
			if (tx.isActive())
				tx.rollback();
		}
	}

	public void registerEvent(@NonNull AbstractEvent event) {
		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			pm.makePersistent(event);
			tx.commit();
		} finally {
			if (tx.isActive())
				tx.rollback();
		}
	}

	public void deleteEventById(@NonNull String id) {
		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			pm.deletePersistent(pm.getObjectById(AbstractEvent.class, id));
			tx.commit();
		} finally {
			if (tx.isActive())
				tx.rollback();
		}
	}

	public void deleteExpiredEvents() {
		Date death = new DateTime().plusDays(-expiryAgeDays).toDate();
		death = new Timestamp(death.getTime()); // UGLY SQL HACK
		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			@SuppressWarnings("unchecked")
			List<String> ids = (List<String>) pm.newNamedQuery(
					AbstractEvent.class, "eventsFromBefore").execute(death);
			log.debug("found " + ids.size()
					+ " events to be squelched (older than " + death + ")");
			for (String id : ids)
				pm.deletePersistent(pm.getObjectById(AbstractEvent.class, id));
			tx.commit();
		} finally {
			if (tx.isActive())
				tx.rollback();
		}
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public void dispatch(TavernaRun originator, String messageSubject,
			String messageContent, String targetParameter) throws Exception {
		UsernamePrincipal owner = originator.getSecurityContext().getOwner();
		UriBuilder ub = ubf.getRunUriBuilder(originator);
		registerEvent(new TerminationEvent(ub.build(), owner, messageSubject,
				messageContent));
	}
}
