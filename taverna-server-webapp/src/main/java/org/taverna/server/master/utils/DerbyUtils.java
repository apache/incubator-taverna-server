package org.taverna.server.master.utils;
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