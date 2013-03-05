/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.remote;

/**
 * Exception that indicates that the implementation is still working on
 * processing the operation. Note that though this is an exception, it is <i>not
 * a failure</i>.
 * 
 * @author Donal Fellows
 */
public class StillWorkingOnItException extends Exception {
	public StillWorkingOnItException(String string) {
		super(string);
	}
}
