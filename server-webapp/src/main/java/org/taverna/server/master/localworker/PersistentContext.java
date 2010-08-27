package org.taverna.server.master.localworker;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.annotations.PersistenceAware;

/**
 * Class that handles the JDO persistence manager, managing transactions for
 * higher-level code.
 * 
 * @author Donal Fellows
 * 
 * @param <T>
 *            The type of persistent objects being handled by this context.
 */
@PersistenceAware
class PersistentContext<T> {
	private PersistenceManager pm;

	public PersistentContext(PersistenceManagerFactory persistenceManagerFactory) {
		pm = persistenceManagerFactory.getPersistenceManager();
	}

	public interface Action<Exn extends Exception> {
		public void act() throws Exn;
	}

	public interface Function<Result, Exn extends Exception> {
		public Result act() throws Exn;
	}

	@SuppressWarnings("unchecked")
	private T singleQuery(Query q) {
		q.setUnique(true);
		Object o = q.execute();
		if (o == null)
			return null;
		return (T) o;
	}

	public T getByID(Class<? extends T> cls, int id) {
		return singleQuery(pm.newQuery(cls, "this.ID == " + id));
	}

	public <Exn extends Exception> void inTransaction(Action<Exn> act)
			throws Exn {
		pm.currentTransaction().begin();
		boolean ok = false;
		try {
			act.act();
			pm.currentTransaction().commit();
			ok = true;
		} finally {
			if (!ok)
				pm.currentTransaction().rollback();
		}
	}

	public <Result, Exn extends Exception> Result inTransaction(
			Function<Result, Exn> act) throws Exn {
		pm.currentTransaction().begin();
		boolean ok = false;
		try {
			Result r = act.act();
			pm.currentTransaction().commit();
			ok = true;
			return r;
		} finally {
			if (!ok)
				pm.currentTransaction().rollback();
		}
	}

	public T persist(T value) {
		return pm.makePersistent(value);
	}
}