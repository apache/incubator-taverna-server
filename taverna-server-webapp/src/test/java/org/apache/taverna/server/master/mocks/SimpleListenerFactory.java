package org.apache.taverna.server.master.mocks;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.taverna.server.master.exceptions.NoListenerException;
import org.apache.taverna.server.master.factories.ListenerFactory;
import org.apache.taverna.server.master.interfaces.Listener;
import org.apache.taverna.server.master.interfaces.TavernaRun;

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
