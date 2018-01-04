package org.apache.taverna.server.client;
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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Connect to a Taverna Server.
 * <p>
 * To create a connected {@link TavernaServer} instance,use
 * {@link #connectNoAuth(URI)} for anonymous connection, or
 * {@link #connectAuth(URI, String, String)}) for authenticated access.
 *
 */
public class TavernaServerConnectionFactory {
	private Map<URI, TavernaServer> cache = new HashMap<>();

	/**
	 * Connect to the Taverna Server without authentication.
	 * <p>
	 * The connection will be anonymous, but can later be made authenticated using
	 * {@link TavernaServer#upgradeToAuth(String, String)}.
	 * 
	 * @param uri
	 *            URI of Taverna Server REST endpoint, e.g.
	 *            <code>http://localhost:8080/taverna-server/rest</code>
	 * @return A configured {@link TavernaServer} instance
	 */
	public synchronized TavernaServer connectNoAuth(URI uri) {
		TavernaServer conn = cache.get(uri);
		if (conn == null)
			cache.put(uri, conn = new TavernaServer(uri));
		return conn;
	}

	/**
	 * Connect to the Taverna Server with the given authentication.
	 * 
	 * @param uri
	 *            URI of Taverna Server REST endpoint, e.g.
	 *            <code>http://localhost:8080/taverna-server/rest</code>
	 * @param username Username
	 * @param password Password
	 * @return A configured {@link TavernaServer} instance
	 */
	public TavernaServer connectAuth(URI uri, String username, String password) {
		TavernaServer conn = new TavernaServer(uri, username, password);
		// Force a check of the credentials by getting the server version
		conn.getServerVersionInfo();
		return conn;
	}
}
