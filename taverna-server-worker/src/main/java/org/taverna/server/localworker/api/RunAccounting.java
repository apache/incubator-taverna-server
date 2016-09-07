/*
 */
package org.taverna.server.localworker.api;

/**
 * 
 * @author Donal Fellows
 */
public interface RunAccounting {
	/**
	 * Logs that a run has started executing.
	 */
	void runStarted();

	/**
	 * Logs that a run has finished executing.
	 */
	void runCeased();
}
