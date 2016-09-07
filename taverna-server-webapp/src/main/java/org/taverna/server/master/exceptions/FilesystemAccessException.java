/*
 */
package org.taverna.server.master.exceptions;
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

import java.rmi.RemoteException;

import javax.xml.ws.WebFault;

/**
 * An exception that happened when the underlying filesystem was accessed.
 * @author Donal Fellows
 */
@WebFault(name = "FilesystemAccessFault")
public class FilesystemAccessException extends Exception {
	private static final long serialVersionUID = 8715937300989820318L;

	public FilesystemAccessException(String msg) {
		super(msg);
	}

	public FilesystemAccessException(String string, Throwable cause) {
		super(string, getRealCause(cause));
	}

	private static Throwable getRealCause(Throwable t) {
		if (t instanceof RemoteException) {
			RemoteException remote = (RemoteException) t;
			if (remote.detail != null)
				return remote.detail;
		}
		if (t.getCause() != null)
			return t.getCause();
		return t;
	}
}