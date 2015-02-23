/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
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
public class LocalWorkerFactory {
	@Bean(name = "localworker.factory")
	AbstractRemoteRunFactory getLocalworkerFactory(
			@Value("${backEndFactory}") String mode) throws Exception {
		if (mode == null || mode.isEmpty() || mode.startsWith("${"))
			throw new Exception("no value for ${backEndFactory}");
		Class<?> c = Class.forName(mode);
		if (AbstractRemoteRunFactory.class.isAssignableFrom(c))
			return (AbstractRemoteRunFactory) c.newInstance();
		throw new Exception("unknown remote run factory: " + mode);
	}
}
