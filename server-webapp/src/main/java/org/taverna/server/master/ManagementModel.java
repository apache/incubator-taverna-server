package org.taverna.server.master;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/** The persistent, manageable state of the Taverna Server web application. */
@PersistenceAware
public class ManagementModel {
	/**
	 * @param persistenceManagerFactory
	 *            The JDO engine to use for managing persistence of the state.
	 */
	public void setPersistenceManagerFactory(
			PersistenceManagerFactory persistenceManagerFactory) {
		this.persistenceManager = persistenceManagerFactory
				.getPersistenceManager();
	}

	private PersistenceManager persistenceManager;

	/** Whether we should log all workflows sent to us. */
	private boolean logIncomingWorkflows = false;

	/** Whether we allow the creation of new workflow runs. */
	private boolean allowNewWorkflowRuns = true;

	/**
	 * Whether outgoing exceptions should be logged before being converted to
	 * responses.
	 */
	private boolean logOutgoingExceptions = false;

	/**
	 * @param logIncomingWorkflows
	 *            whether we should log all workflows sent to us.
	 */
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		this.logIncomingWorkflows = logIncomingWorkflows;
		store();
	}

	/**
	 * @return whether we should log all workflows sent to us.
	 */
	public boolean getLogIncomingWorkflows() {
		load();
		return logIncomingWorkflows;
	}

	/**
	 * @param allowNewWorkflowRuns
	 *            whether we allow the creation of new workflow runs.
	 */
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
		this.allowNewWorkflowRuns = allowNewWorkflowRuns;
		store();
	}

	/**
	 * @return whether we allow the creation of new workflow runs.
	 */
	public boolean getAllowNewWorkflowRuns() {
		load();
		return allowNewWorkflowRuns;
	}

	/**
	 * @param logOutgoingExceptions
	 *            whether outgoing exceptions should be logged before being
	 *            converted to responses.
	 */
	public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
		this.logOutgoingExceptions = logOutgoingExceptions;
		store();
	}

	/**
	 * @return whether outgoing exceptions should be logged before being
	 *         converted to responses.
	 */
	public boolean getLogOutgoingExceptions() {
		load();
		return logOutgoingExceptions;
	}

	private static final int KEY = 42; // whatever

	@SuppressWarnings("unchecked")
	private WebappState get() {
		Query q = persistenceManager
				.newQuery(WebappState.class, "id == " + KEY);
		Collection<WebappState> results = (Collection<WebappState>) q.execute();
		if (results.isEmpty()) {
			return null;
		}
		return results.iterator().next();
	}

	private boolean loadedState;
	public void load() {
		if (loadedState || persistenceManager == null)
			return;
		boolean ok = false;
		try {
			persistenceManager.currentTransaction().begin();
			WebappState state = get();
			if (state == null)
				return;
			allowNewWorkflowRuns = state.getAllowNewWorkflowRuns();
			logIncomingWorkflows = state.getLogIncomingWorkflows();
			logOutgoingExceptions = state.getLogOutgoingExceptions();
			persistenceManager.currentTransaction().commit();
			ok = true;
			loadedState = true;
		} finally {
			if (!ok)
				persistenceManager.currentTransaction().rollback();
		}
	}

	private void store() {
		if (persistenceManager == null)
			return;
		boolean ok = false;
		try {
			persistenceManager.currentTransaction().begin();
			WebappState state = get();
			if (state == null) {
				state = new WebappState();
				// save state
				state.id = KEY; // whatever...
				state.setAllowNewWorkflowRuns(allowNewWorkflowRuns);
				state.setLogIncomingWorkflows(logIncomingWorkflows);
				state.setLogOutgoingExceptions(logOutgoingExceptions);
				state = persistenceManager.makePersistent(state);
			} else {
				state.setAllowNewWorkflowRuns(allowNewWorkflowRuns);
				state.setLogIncomingWorkflows(logIncomingWorkflows);
				state.setLogOutgoingExceptions(logOutgoingExceptions);
			}
			persistenceManager.currentTransaction().commit();
			ok = true;
			loadedState = true;
		} finally {
			if (!ok)
				persistenceManager.currentTransaction().rollback();
		}
	}

	@PersistenceCapable(table="MANAGEMENTMODEL__WEBAPPSTATE")
	static class WebappState {
		@PrimaryKey
		protected int id;

		/** Whether we should log all workflows sent to us. */
		@Persistent
		private boolean logIncomingWorkflows;

		/** Whether we allow the creation of new workflow runs. */
		@Persistent
		private boolean allowNewWorkflowRuns;

		/**
		 * Whether outgoing exceptions should be logged before being converted
		 * to responses.
		 */
		@Persistent
		private boolean logOutgoingExceptions;

		/**
		 * @param logIncomingWorkflows the logIncomingWorkflows to set
		 */
		public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
			this.logIncomingWorkflows = logIncomingWorkflows;
		}

		/**
		 * @return the logIncomingWorkflows
		 */
		public boolean getLogIncomingWorkflows() {
			return logIncomingWorkflows;
		}

		/**
		 * @param allowNewWorkflowRuns the allowNewWorkflowRuns to set
		 */
		public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
			this.allowNewWorkflowRuns = allowNewWorkflowRuns;
		}

		/**
		 * @return the allowNewWorkflowRuns
		 */
		public boolean getAllowNewWorkflowRuns() {
			return allowNewWorkflowRuns;
		}

		/**
		 * @param logOutgoingExceptions the logOutgoingExceptions to set
		 */
		public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
			this.logOutgoingExceptions = logOutgoingExceptions;
		}

		/**
		 * @return the logOutgoingExceptions
		 */
		public boolean getLogOutgoingExceptions() {
			return logOutgoingExceptions;
		}
	}
}
