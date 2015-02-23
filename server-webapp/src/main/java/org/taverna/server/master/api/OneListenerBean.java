package org.taverna.server.master.api;

import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerListenersREST.TavernaServerListenerREST;

/**
 * Description of properties supported by {@link InputREST}.
 * 
 * @author Donal Fellows
 */
public interface OneListenerBean {
	TavernaServerListenerREST connect(Listener listen, TavernaRun run);
}