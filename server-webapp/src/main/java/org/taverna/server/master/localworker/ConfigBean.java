/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider of the configuration of the "localworker.factory" bean, which is
 * sufficiently complex to be too hard to manufacture directly from the XML
 * configuration.
 * 
 * @author Donal Fellows
 */
@Configuration
public class ConfigBean {
	@Bean(name = "localworker.factory")
	AbstractRemoteRunFactory getLocalworkerFactory(
			@Value("${backEndFactory}") String mode) throws Exception {
		AbstractRemoteRunFactory factory;
		if (mode == null)
			throw new Exception("no value for ${backEndFactory}");
		if ("org.taverna.server.master.localworker.IdAwareForkRunFactory"
				.equals(mode))
			factory = new IdAwareForkRunFactory();
		else if ("org.taverna.server.master.localworker.ForkRunFactory"
				.equals(mode))
			factory = new ForkRunFactory();
		else
			throw new Exception("unknown remote run factory: " + mode);
		return factory;
	}
}
