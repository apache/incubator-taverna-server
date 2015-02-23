package org.taverna.server.master.api;

import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.TavernaServerSecurityREST;

/**
 * Description of properties supported by {@link RunSecurityREST}.
 * 
 * @author Donal Fellows
 */
public interface SecurityBean extends SupportAware {
	TavernaServerSecurityREST connect(TavernaSecurityContext context, TavernaRun run);
}