/*
 */
package org.apache.taverna.server.master.localworker;
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
