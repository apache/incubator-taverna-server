package org.taverna.server.master.utils;

import javax.annotation.PreDestroy;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerImpl;

/**
 * Simple support class that wraps up and provides access to the correct parts
 * of JDO.
 * 
 * @author Donal Fellows
 * 
 * @param <T>
 *            The context class that the subclass will be working with.
 */
public abstract class JDOSupport<T> {
	private Class<T> contextClass;
	private PersistenceManager pm;

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
	public final void setPersistenceManagerFactory(
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
	 */
	protected boolean isPersistent() {
		return pm != null;
	}

	/**
	 * Start executing a transaction.
	 * 
	 * @return The transaction, which must be manually committed or rolled back.
	 */
	protected Xact beginTx() {
		final Transaction tx = pm.currentTransaction();
		if (tx.isActive()) {
			TavernaServerImpl.log
					.warn("starting transaction when transaction is already started?!");
			// Return a dummy that does nothing...
			return new Xact() {
				@Override
				public void commit() {
				}

				@Override
				public void rollback() {
				}
			};
		}
		tx.begin();
		// TavernaServerImpl.log.info("started transaction " + tx +
		// " for class " + getClass());
		return new Xact() {
			@Override
			public void commit() {
				tx.commit();
			}

			@Override
			public void rollback() {
				tx.rollback();
			}
		};
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

	protected interface Xact {
		void commit();

		void rollback();
	}
}
