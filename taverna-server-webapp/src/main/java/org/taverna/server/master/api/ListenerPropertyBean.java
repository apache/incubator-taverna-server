package org.taverna.server.master.api;

import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerListenersREST;

/**
 * Description of properties supported by {@link ListenerPropertyREST}.
 * 
 * @author Donal Fellows
 */
public interface ListenerPropertyBean extends SupportAware {
	TavernaServerListenersREST.Property connect(Listener listen,
			TavernaRun run, String propertyName);
}