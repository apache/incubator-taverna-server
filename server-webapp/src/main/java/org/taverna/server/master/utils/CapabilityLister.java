package org.taverna.server.master.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.taverna.server.master.common.Capability;

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
