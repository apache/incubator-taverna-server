package org.taverna.server.rmidaemon;
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

import static java.lang.System.setProperty;
import static java.net.InetAddress.getLocalHost;
import static java.rmi.registry.LocateRegistry.createRegistry;
import static java.rmi.registry.Registry.REGISTRY_PORT;
import static java.rmi.server.RMISocketFactory.getDefaultSocketFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.rmi.MarshalledObject;
import java.rmi.server.RMIServerSocketFactory;

/**
 * Special version of <tt>rmiregistry</tt>.
 * 
 * @author Donal Fellows
 */
public class Registry {
	/**
	 * Run a registry. The first optional argument is the port for the registry
	 * to listen on, and the second optional argument is whether the registry
	 * should restrict itself to connections from localhost only.
	 * 
	 * @param args
	 *            Arguments to the program.
	 */
	public static void main(String... args) {
		try {
			if (args.length > 0)
				port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("failed to parse port: " + e.getMessage());
			System.exit(2);
		}
		try {
			if (args.length > 1)
				localhostOnly = Boolean.parseBoolean(args[1]);
		} catch (Exception e) {
			System.err.println("failed to parse boolean localhost flag: "
					+ e.getMessage());
			System.exit(2);
		}
		try {
			Object registryHandle = makeRegistry();
			try (ObjectOutputStream oos = new ObjectOutputStream(System.out)) {
				oos.writeObject(registryHandle);
			}
		} catch (Exception e) {
			System.err.println("problem creating registry: " + e.getMessage());
			System.exit(1);
		}
	}

	private static int port = REGISTRY_PORT;
	private static boolean localhostOnly = false;

	private static MarshalledObject<java.rmi.registry.Registry> makeRegistry() throws IOException {
		if (!localhostOnly)
			return new MarshalledObject<>(createRegistry(port));
		setProperty("java.rmi.server.hostname", "127.0.0.1");
		return new MarshalledObject<>(createRegistry(port,
				getDefaultSocketFactory(), new RMIServerSocketFactory() {
					@Override
					public ServerSocket createServerSocket(int port)
							throws IOException {
						return new ServerSocket(port, 0, getLocalHost());
					}
				}));
	}
}
