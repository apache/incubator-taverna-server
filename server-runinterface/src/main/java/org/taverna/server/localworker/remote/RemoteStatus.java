/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.remote;

/**
 * States of a workflow run. They are {@link RemoteStatus#Initialized
 * Initialized}, {@link RemoteStatus#Operating Operating},
 * {@link RemoteStatus#Stopped Stopped}, and {@link RemoteStatus#Finished
 * Finished}. Conceptually, there is also a <tt>Destroyed</tt> state, but the
 * workflow run does not exist (and hence can't have its state queried or set)
 * in that case.
 * 
 * @author Donal Fellows
 */
public enum RemoteStatus {
	/**
	 * The workflow run has been created, but is not yet running. The run will
	 * need to be manually moved to {@link #Operating} when ready.
	 */
	Initialized,
	/**
	 * The workflow run is going, reading input, generating output, etc. Will
	 * eventually either move automatically to {@link #Finished} or can be moved
	 * manually to {@link #Stopped} (where supported).
	 */
	Operating,
	/**
	 * The workflow run is paused, and will need to be moved back to
	 * {@link #Operating} manually.
	 */
	Stopped,
	/**
	 * The workflow run has ceased; data files will continue to exist until the
	 * run is destroyed (which may be manual or automatic).
	 */
	Finished
}
