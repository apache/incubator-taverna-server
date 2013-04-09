/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.logging.LogFactory.getLog;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.annotation.PreDestroy;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Required;

/**
 * Simple support class that wraps up and provides access to the correct parts
 * of JDO.
 * 
 * @author Donal Fellows
 * 
 * @param &lt;T&gt; The context class that the subclass will be working with.
 */
public abstract class JDOSupport<T> {
	private Class<T> contextClass;
	PersistenceManager pm;

	/**
	 * Instantiate this class, supplying it a handle to the class that will be
	 * used to provide context for queries and accesses.
	 * 
	 * @param contextClass
	 *            Must match the type parameter to the class itself.
	 */
	protected JDOSupport(Class<T> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence.
	 */
	@Required
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		pm = persistenceManagerFactory.getPersistenceManagerProxy();
	}

	@PreDestroy
	public void close() {
		pm.close();
	}

	/**
	 * Has this class actually been configured with a persistence manager by
	 * Spring?
	 * 
	 * @return Whether there is a persistence manager installed.
	 */
	protected boolean isPersistent() {
		return pm != null;
	}

	/**
	 * Get an instance of a query in JDOQL.
	 * 
	 * @param filter
	 *            The filter part of the query.
	 * @return The query, which should be executed to retrieve the results.
	 */
	protected Query query(String filter) {
		return pm.newQuery(contextClass, filter);
	}

	/**
	 * Get an instance of a named query attached to the context class (as an
	 * annotation).
	 * 
	 * @param name
	 *            The name of the query.
	 * @return The query, which should be executed to retrieve the results.
	 * @see javax.jdo.annotations.Query
	 */
	protected Query namedQuery(String name) {
		return pm.newNamedQuery(contextClass, name);
	}

	/**
	 * Make an instance of the context class persist in the database. It's
	 * identity must not already exist.
	 * 
	 * @param value
	 *            The instance to persist.
	 * @return The persistence-coupled instance.
	 */
	protected T persist(T value) {
		return pm.makePersistent(value);
	}

	/**
	 * Make a non-persistent (i.e., will hold its value past the end of the
	 * transaction) copy of a persistence-coupled instance of the context class.
	 * 
	 * @param value
	 *            The value to decouple.
	 * @return The non-persistent copy.
	 */
	protected T detach(T value) {
		return pm.detachCopy(value);
	}

	/**
	 * Look up an instance of the context class by its identity.
	 * 
	 * @param id
	 *            The identity of the object.
	 * @return The instance, which is persistence-coupled.
	 */
	protected T getById(Object id) {
		try {
			return pm.getObjectById(contextClass, id);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Delete a persistence-coupled instance of the context class.
	 * 
	 * @param value
	 *            The value to delete.
	 */
	protected void delete(T value) {
		pm.deletePersistent(value);
	}

	/**
	 * Manages integration of JDO transactions with Spring.
	 * 
	 * @author Donal Fellows
	 */
	@Aspect
	public static class TransactionAspect {
		private Object lock = new Object();

		@Around(value = "@annotation(org.taverna.server.master.utils.JDOSupport.WithinSingleTransaction) && target(support)", argNames = "support")
		Object applyTransaction(ProceedingJoinPoint pjp, JDOSupport<?> support)
				throws Throwable {
			synchronized (lock) {
				Transaction tx = support.pm == null ? null : support.pm
						.currentTransaction();
				if (tx != null && tx.isActive())
					tx = null;
				if (tx != null)
					tx.begin();
				try {
					Object result = pjp.proceed();
					if (tx != null)
						tx.commit();
					tx = null;
					return result;
				} catch (Throwable t) {
					try {
						if (tx != null)
							tx.rollback();
					} catch (JDOException e) {
						getLog("Taverna.Server.Utils").warn("rollback failed unexpectedly",
								e);
					}
					throw t;
				}
			}
		}
	}

	/**
	 * Mark a method (of a subclass of {@link JDOSupport}) as having a
	 * transaction wrapped around it. The transactions are managed correctly in
	 * the multi-threaded case.
	 * 
	 * @author Donal Fellows
	 */
	@Target(METHOD)
	@Retention(RUNTIME)
	public @interface WithinSingleTransaction {
	}
}
