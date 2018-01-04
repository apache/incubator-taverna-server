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

import org.apache.taverna.server.client.TavernaServer.ClientException;
import org.apache.taverna.server.client.TavernaServer.ServerException;

import com.sun.jersey.api.client.ClientResponse;

abstract class Connected {
	void checkError(ClientResponse response) throws ClientException,
			ServerException {
		ClientResponse.Status s = response.getClientResponseStatus();
		if (s.getStatusCode() == 401)
			throw new TavernaServer.AuthorizationException("not authorized",
					null);
		if (s.getStatusCode() >= 500)
			throw new TavernaServer.ServerException(s.getReasonPhrase(), null);
		if (s.getStatusCode() >= 400)
			throw new TavernaServer.ClientException(s.getReasonPhrase(), null);
	}
}
