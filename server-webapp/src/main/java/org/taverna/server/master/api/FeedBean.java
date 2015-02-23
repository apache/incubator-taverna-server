package org.taverna.server.master.api;

import org.taverna.server.master.InteractionFeed;
import org.taverna.server.master.interaction.InteractionFeedSupport;

/**
 * Description of properties supported by {@link InteractionFeed}.
 * 
 * @author Donal Fellows
 */
public interface FeedBean {
	void setInteractionFeedSupport(InteractionFeedSupport feed);
}