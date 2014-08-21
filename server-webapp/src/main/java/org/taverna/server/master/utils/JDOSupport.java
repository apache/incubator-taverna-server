/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.logging.LogFactory.getLog;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.apache.commons.logging.Log;
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
	private PersistenceManagerBuilder pmb;

	/**
	 * Instantiate this class, supplying it a handle to the class that will be
	 * used to provide context for queries and accesses.
	 * 
	 * @param contextClass
	 *            Must match the type parameter to the class itself.
	 */
	protected JDOSupport(@Nonnull Class<T> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * @param persistenceManagerBuilder
	 *            The JDO engine to use for managing persistence.
	 */
	@Required
	public void setPersistenceManagerBuilder(
			PersistenceManagerBuilder persistenceManagerBuilder) {
		pmb = persistenceManagerBuilder;
	}

	private PersistenceManager pm() {
		if (isPersistent())
			return pmb.getPersistenceManager();
		return null;
	}

	/**
	 * Has this class actually been configured with a persistence manager by
	 * Spring?
	 * 
	 * @return Whether there is a persistence manager installed.
	 */
	protected boolean isPersistent() {
		return pmb != null;
	}

	/**
	 * Get an instance of a query in JDOQL.
	 * 
	 * @param filter
	 *            The filter part of the query.
	 * @return The query, which should be executed to retrieve the results.
	 */
	@Nonnull
	protected Query query(@Nonnull String filter) {
		return pm().newQuery(contextClass, filter);
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
	@Nonnull
	protected Query namedQuery(@Nonnull String name) {
		return pm().newNamedQuery(contextClass, name);
	}

	/**
	 * Make an instance of the context class persist in the database. It's
	 * identity must not already exist.
	 * 
	 * @param value
	 *            The instance to persist.
	 * @return The persistence-coupled instance.
	 */
	@Nullable
	protected T persist(@Nullable T value) {
		if (value == null)
			return null;
		return pm().makePersistent(value);
	}

	/**
	 * Make a non-persistent (i.e., will hold its value past the end of the
	 * transaction) copy of a persistence-coupled instance of the context class.
	 * 
	 * @param value
	 *            The value to decouple.
	 * @return The non-persistent copy.
	 */
	@Nullable
	protected T detach(@Nullable T value) {
		if (value == null)
			return null;
		return pm().detachCopy(value);
	}

	/**
	 * Look up an instance of the context class by its identity.
	 * 
	 * @param id
	 *            The identity of the object.
	 * @return The instance, which is persistence-coupled.
	 */
	@Nullable
	protected T getById(Object id) {
		try {
			return pm().getObjectById(contextClass, id);
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
	protected void delete(@Nullable T value) {
		if (value != null)
			pm().deletePersistent(value);
	}

	/**
	 * Manages integration of JDO transactions with Spring.
	 * 
	 * @author Donal Fellows
	 */
	@Aspect
	public static class TransactionAspect {
		private Object lock = new Object();
		private Log log = getLog("Taverna.Server.Utils");
		private volatile int txid;

		@Around(value = "@annotation(org.taverna.server.master.utils.JDOSupport.WithinSingleTransaction) && target(support)", argNames = "support")
		Object applyTransaction(ProceedingJoinPoint pjp, JDOSupport<?> support)
				throws Throwable {
			synchronized (lock) {
				PersistenceManager pm = support.pm();
				int id = ++txid;
				Transaction tx = (pm == null) ? null : pm.currentTransaction();
				if (tx != null && tx.isActive())
					tx = null;
				if (tx != null) {
					if (log.isDebugEnabled())
						log.debug("starting transaction #" + id);
					tx.begin();
				}
				try {
					Object result = pjp.proceed();
					if (tx != null) {
						tx.commit();
						if (log.isDebugEnabled())
							log.debug("committed transaction #" + id);
					}
					tx = null;
					return result;
				} catch (Throwable t) {
					try {
						if (tx != null) {
							tx.rollback();
							if (log.isDebugEnabled())
								log.debug("rolled back transaction #" + id);
						}
					} catch (JDOException e) {
						log.warn("rollback failed unexpectedly", e);
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
	@Documented
	public @interface WithinSingleTransaction {
	}

	public static class PersistenceManagerBuilder {
		private PersistenceManagerFactory pmf;
		private WeakHashMap<Thread, PersistenceManager> cache = new WeakHashMap<>();

		/**
		 * @param persistenceManagerFactory
		 *            The JDO engine to use for managing persistence.
		 */
		@Required
		public void setPersistenceManagerFactory(
				PersistenceManagerFactory persistenceManagerFactory) {
			pmf = persistenceManagerFactory;
		}

		@Nonnull
		public PersistenceManager getPersistenceManager() {
			if (cache == null)
				return pmf.getPersistenceManager();
			Thread t = Thread.currentThread();
			PersistenceManager pm = cache.get(t);
			if (pm == null && pmf != null) {
				pm = pmf.getPersistenceManager();
				cache.put(t, pm);
			}
			return pm;
		}

		@PreDestroy
		void clearThreadCache() {
			WeakHashMap<Thread, PersistenceManager> cache = this.cache;
			this.cache = null;
			for (PersistenceManager pm : cache.values())
				if (pm != null)
					pm.close();
			cache.clear();
		}
	}
}
