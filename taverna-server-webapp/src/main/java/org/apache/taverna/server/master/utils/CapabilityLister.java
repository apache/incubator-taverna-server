package org.apache.taverna.server.master.utils;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.taverna.server.master.common.Capability;

/**
 * Utility for listing the capabilities supported by this Taverna Server
 * installation.
 * 
 * @author Donal Fellows
 */
public class CapabilityLister {
	public static final String CAPABILITY_RESOURCE_FILE = "/capabilities.properties";
	private Properties properties = new Properties();

	@PostConstruct
	void loadCapabilities() throws IOException {
		try (InputStream is = getClass().getResourceAsStream(
				CAPABILITY_RESOURCE_FILE)) {
			if (is != null)
				properties.load(is);
		}
	}

	public List<Capability> getCapabilities() {
		List<Capability> caps = new ArrayList<>();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			Capability c = new Capability();
			c.capability = URI.create(entry.getKey().toString());
			c.version = entry.getValue().toString();
			caps.add(c);
		}
		return caps;
	}
}
