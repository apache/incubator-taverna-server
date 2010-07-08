package org.taverna.server.master.interfaces;

import java.security.Principal;

// TODO fill this out
/**
 * Outline of the security context for a workflow run.
 * 
 * @author Donal Fellows
 */
public interface TavernaSecurityContext {
	/**
	 * @return Who owns the security context.
	 */
	public Principal getOwner();
}
