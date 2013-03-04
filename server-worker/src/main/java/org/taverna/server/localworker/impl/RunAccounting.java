/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

/**
 * How the accounting for workflow runs is handled.
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
