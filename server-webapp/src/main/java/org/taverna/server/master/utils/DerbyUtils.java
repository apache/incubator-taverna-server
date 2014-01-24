package org.taverna.server.master.utils;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class, used to make Derby less broken.
 * 
 * @see http://stackoverflow.com/questions/1004327/getting-rid-of-derby-log
 * @see http://stackoverflow.com/questions/3339736/set-system-property-with-spring-configuration-file
 */
public class DerbyUtils {
	/**
	 * A writer that channels things on to the log.
	 */
	public static final Writer TO_LOG = new Writer() {
		Log log = LogFactory.getLog("Taverna.Server.Database");
		StringBuilder sb = new StringBuilder();

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			if (!log.isInfoEnabled())
				return;
			sb.append(cbuf, off, len);
			while (true) {
				int idx = sb.indexOf("\n");
				if (idx < 0)
					break;
				if (idx > 0 && sb.charAt(idx - 1) == '\r')
					idx--;
				log.info(sb.substring(0, idx));
				sb.delete(0, idx + 1);
			}
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}
	};
	// Hack
	public static final Writer DEV_NULL = TO_LOG;
}
