package org.taverna.server.master.utils;

import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class FlushThreadLocalCacheInterceptor extends
		AbstractPhaseInterceptor<Message> {
	public FlushThreadLocalCacheInterceptor() {
		super(Phase.USER_LOGICAL_ENDING);
	}

	@Override
	public void handleMessage(Message message) {
		ProviderFactory.getInstance(message).clearThreadLocalProxies();
	}
}
