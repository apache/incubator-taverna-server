package org.taverna.server.master;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/** The persistent, manageable state of the Taverna Server web application. */
@PersistenceAware
class ManagementState implements ManagementModel {
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

	@Override
	public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
		this.logIncomingWorkflows = logIncomingWorkflows;
		store();
	}

	@Override
	public boolean getLogIncomingWorkflows() {
		load();
		return logIncomingWorkflows;
	}

	@Override
	public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
		this.allowNewWorkflowRuns = allowNewWorkflowRuns;
		store();
	}

	@Override
	public boolean getAllowNewWorkflowRuns() {
		load();
		return allowNewWorkflowRuns;
	}

	@Override
	public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
		this.logOutgoingExceptions = logOutgoingExceptions;
		store();
	}

	@Override
	public boolean getLogOutgoingExceptions() {
		load();
		return logOutgoingExceptions;
	}

	private static final int KEY = 42; // whatever

	private WebappState get() {
		Query q = persistenceManager
				.newQuery(WebappState.class, "id == " + KEY);
		q.setUnique(true);
		return (WebappState) q.execute();
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
				state = persistenceManager.makePersistent(state);
			}
			state.setAllowNewWorkflowRuns(allowNewWorkflowRuns);
			state.setLogIncomingWorkflows(logIncomingWorkflows);
			state.setLogOutgoingExceptions(logOutgoingExceptions);
			persistenceManager.currentTransaction().commit();
			ok = true;
			loadedState = true;
		} finally {
			if (!ok)
				persistenceManager.currentTransaction().rollback();
		}
	}

	@PersistenceCapable(table="MANAGEMENTSTATE__WEBAPPSTATE")
	private static class WebappState implements ManagementModel {
		public WebappState() {}

		@PrimaryKey
		@SuppressWarnings("unused") // Is used, but Eclipse doesn't know
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

		@Override
		public void setLogIncomingWorkflows(boolean logIncomingWorkflows) {
			this.logIncomingWorkflows = logIncomingWorkflows;
		}

		@Override
		public boolean getLogIncomingWorkflows() {
			return logIncomingWorkflows;
		}

		@Override
		public void setAllowNewWorkflowRuns(boolean allowNewWorkflowRuns) {
			this.allowNewWorkflowRuns = allowNewWorkflowRuns;
		}

		@Override
		public boolean getAllowNewWorkflowRuns() {
			return allowNewWorkflowRuns;
		}

		@Override
		public void setLogOutgoingExceptions(boolean logOutgoingExceptions) {
			this.logOutgoingExceptions = logOutgoingExceptions;
		}

		@Override
		public boolean getLogOutgoingExceptions() {
			return logOutgoingExceptions;
		}
	}
}
