/*
 */
package org.apache.taverna.server.master.identity;
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

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.taverna.server.master.interfaces.LocalIdentityMapper;
import org.apache.taverna.server.master.utils.UsernamePrincipal;

/**
 * An identity mapper that composes the results from other mappers, using the
 * identity mappers in order until one can provide a non-<tt>null</tt> answer.
 * 
 * @author Donal Fellows.
 */
public class CompositeIDMapper implements LocalIdentityMapper,
		ApplicationContextAware {
	private Log log = getLog("Taverna.Server.IdentityMapper");
	private List<LocalIdentityMapper> mappers;
	private ApplicationContext context;

	/**
	 * @param mappers
	 *            The list of mappers to delegate to. Order is significant.
	 */
	public void setIdentityMappers(List<LocalIdentityMapper> mappers) {
		this.mappers = mappers;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		context = applicationContext;
	}

	@Override
	public String getUsernameForPrincipal(UsernamePrincipal user) {
		if (mappers == null)
			return null;
		for (LocalIdentityMapper m : mappers) {
			String u = m.getUsernameForPrincipal(user);
			if (u == null)
				continue;
			for (Entry<String, ? extends LocalIdentityMapper> entry : context
					.getBeansOfType(m.getClass()).entrySet())
				if (m == entry.getValue()) {
					log.info("used " + entry.getKey() + " LIM to map " + user
							+ " to " + u);
					break;
				}
			return u;
		}
		return null;
	}
}
