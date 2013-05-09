package org.taverna.server.master.utils;

import java.io.OutputStream;

/**
 * Utility class, used to make Derby less broken.
 * @see http://stackoverflow.com/questions/1004327/getting-rid-of-derby-log
 * @see http://stackoverflow.com/questions/3339736/set-system-property-with-spring-configuration-file
 */
public class DerbyUtils {
	public static final OutputStream DEV_NULL = new OutputStream() {
		@Override
		public void write(int b) {
			// Do nothing; we're throwing it away
		}
	};
}
