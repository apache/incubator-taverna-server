package org.taverna.server.master.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class, used to make Derby less broken.
 * 
 * @see <a
 *      href="http://stackoverflow.com/questions/1004327/getting-rid-of-derby-log">
 *      Getting rid of derby.log </a>
 * @see <a
 *      href="http://stackoverflow.com/questions/3339736/set-system-property-with-spring-configuration-file">
 *      Set system property with Spring configuration file </a>
 */
public class DerbyUtils {
	/**
	 * A writer that channels things on to the log.
	 */
	public static final Writer TO_LOG = new DBLog();
	// Hack
	public static final Writer DEV_NULL = TO_LOG;
}

class DBLog extends Writer {
	private Log log = LogFactory.getLog("Taverna.Server.Database");
	private StringBuilder sb = new StringBuilder();
	private boolean closed = false;

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if (closed)
			throw new EOFException();
		if (!log.isInfoEnabled())
			return;
		sb.append(cbuf, off, len);
		while (!closed) {
			int idx = sb.indexOf("\n"), realIdx = idx;
			if (idx < 0)
				break;
			char ch;
			while (idx > 0 && ((ch = sb.charAt(idx - 1)) == '\r' || ch == ' ' || ch == '\t'))
				idx--;
			if (idx > 0)
				log.info(sb.substring(0, idx));
			sb.delete(0, realIdx + 1);
		}
	}

	@Override
	public void flush() throws IOException {
		if (sb.length() > 0) {
			log.info(sb.toString());
			sb = new StringBuilder();
		}
	}

	@Override
	public void close() throws IOException {
		flush();
		closed = true;
		sb = null;
	}
}