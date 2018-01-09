/*
 */
package org.apache.taverna.server.master.api;
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

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Required;
import org.apache.taverna.server.master.ContentsDescriptorBuilder;
import org.apache.taverna.server.master.TavernaServerSupport;
import org.apache.taverna.server.master.interfaces.Policy;
import org.apache.taverna.server.master.interfaces.RunStore;
import org.apache.taverna.server.master.interfaces.TavernaSecurityContext;
import org.apache.taverna.server.master.interfaces.UriBuilderFactory;
import org.apache.taverna.server.master.notification.NotificationEngine;
import org.apache.taverna.server.master.notification.atom.EventDAO;
import org.apache.taverna.server.master.rest.TavernaServerREST;
import org.apache.taverna.server.master.soap.TavernaServerSOAP;
import org.apache.taverna.server.master.utils.FilenameUtils;

/**
 * The methods of the webapp that are accessed by beans other than itself or
 * those which are told directly about it. This exists so that an AOP proxy can
 * be installed around it.
 * 
 * @author Donal Fellows
 */
public interface TavernaServerBean extends TavernaServerSOAP, TavernaServerREST,
		UriBuilderFactory {
	/**
	 * @param policy
	 *            The policy being installed by Spring.
	 */
	@Required
	void setPolicy(@Nonnull Policy policy);

	/**
	 * @param runStore
	 *            The run store being installed by Spring.
	 */
	@Required
	void setRunStore(@Nonnull RunStore runStore);

	/**
	 * @param converter
	 *            The filename converter being installed by Spring.
	 */
	@Required
	void setFileUtils(@Nonnull FilenameUtils converter);

	/**
	 * @param cdBuilder
	 *            The contents descriptor builder being installed by Spring.
	 */
	@Required
	void setContentsDescriptorBuilder(
			@Nonnull ContentsDescriptorBuilder cdBuilder);

	/**
	 * @param notificationEngine
	 *            The notification engine being installed by Spring.
	 */
	@Required
	void setNotificationEngine(@Nonnull NotificationEngine notificationEngine);

	/**
	 * @param support
	 *            The support bean being installed by Spring.
	 */
	@Required
	void setSupport(@Nonnull TavernaServerSupport support);

	/**
	 * @param eventSource
	 *            The event source bean being installed by Spring.
	 */
	@Required
	void setEventSource(@Nonnull EventDAO eventSource);

	/**
	 * The nastier parts of security initialisation in SOAP calls, which we want
	 * to go away.
	 * 
	 * @param context
	 *            The context to configure.
	 * @return True if we did <i>not</i> initialise things.
	 */
	boolean initObsoleteSOAPSecurity(@Nonnull TavernaSecurityContext context);

	/**
	 * The nastier parts of security initialisation in REST calls, which we want
	 * to go away.
	 * 
	 * @param context
	 *            The context to configure.
	 * @return True if we did <i>not</i> initialise things.
	 */
	boolean initObsoleteRESTSecurity(@Nonnull TavernaSecurityContext context);
}