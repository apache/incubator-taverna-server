/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.interfaces.Policy;
import org.taverna.server.master.interfaces.RunStore;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.notification.NotificationEngine;
import org.taverna.server.master.notification.atom.EventDAO;
import org.taverna.server.master.rest.TavernaServerREST;
import org.taverna.server.master.soap.TavernaServerSOAP;
import org.taverna.server.master.utils.FilenameUtils;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The methods of the webapp that are accessed by beans other than itself or
 * those which are told directly about it. This exists so that an AOP proxy can
 * be installed around it.
 * 
 * @author Donal Fellows
 */
public interface TavernaServer extends TavernaServerSOAP, TavernaServerREST,
		UriBuilderFactory {
	/**
	 * @param policy
	 *            The policy being installed by Spring.
	 */
	@Required
	void setPolicy(@NonNull Policy policy);

	/**
	 * @param runStore
	 *            The run store being installed by Spring.
	 */
	@Required
	void setRunStore(@NonNull RunStore runStore);

	/**
	 * @param converter
	 *            The filename converter being installed by Spring.
	 */
	@Required
	void setFileUtils(@NonNull FilenameUtils converter);

	/**
	 * @param cdBuilder
	 *            The contents descriptor builder being installed by Spring.
	 */
	@Required
	void setContentsDescriptorBuilder(@NonNull ContentsDescriptorBuilder cdBuilder);

	/**
	 * @param notificationEngine
	 *            The notification engine being installed by Spring.
	 */
	@Required
	void setNotificationEngine(@NonNull NotificationEngine notificationEngine);

	/**
	 * @param support
	 *            The support bean being installed by Spring.
	 */
	@Required
	void setSupport(@NonNull TavernaServerSupport support);
	
	/**
	 * @param eventSource
	 *            The event source bean being installed by Spring.
	 */
	@Required
	void setEventSource(@NonNull EventDAO eventSource);

	/**
	 * The nastier parts of security initialisation, which we want to go away.
	 * 
	 * @param context
	 *            The context to configure.
	 */
	void initObsoleteSecurity(@NonNull TavernaSecurityContext context);
}