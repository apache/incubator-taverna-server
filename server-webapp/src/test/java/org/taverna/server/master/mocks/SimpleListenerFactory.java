package org.taverna.server.master.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.factories.ListenerFactory;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * A factory for event listener. The factory is configured using Spring.
 * 
 * @author Donal Fellows
 */
public class SimpleListenerFactory implements ListenerFactory {
	private Map<String, Builder> builders = new HashMap<>();

	public void setBuilders(Map<String, Builder> builders) {
		this.builders = builders;
	}

	@Override
	public List<String> getSupportedListenerTypes() {
		return new ArrayList<>(builders.keySet());
	}

	@Override
	public Listener makeListener(TavernaRun run, String listenerType,
			String configuration) throws NoListenerException {
		Builder b = builders.get(listenerType);
		if (b == null)
			throw new NoListenerException("no such listener type");
		Listener l = b.build(run, configuration);
		run.addListener(l);
		return l;
	}

	/**
	 * How to actually construct a listener.
	 * 
	 * @author Donal Fellows
	 */
	public interface Builder {
		/**
		 * Make an event listener attached to a run.
		 * 
		 * @param run
		 *            The run to attach to.
		 * @param configuration
		 *            A user-specified configuration document. The constructed
		 *            listener <i>should</i> process this configuration document
		 *            and be able to return it to the user when requested.
		 * @return The listener object.
		 * @throws NoListenerException
		 *             If the listener construction failed or the
		 *             <b>configuration</b> document was bad in some way.
		 */
		public Listener build(TavernaRun run, String configuration)
				throws NoListenerException;
	}
}
