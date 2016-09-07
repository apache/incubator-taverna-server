package uk.org.taverna.server.client;
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

import org.taverna.server.client.wadl.TavernaServer.Root.RunsRunName.Wd.Path2;

import uk.org.taverna.server.client.TavernaServer.ClientException;
import uk.org.taverna.server.client.TavernaServer.ServerException;

import com.sun.jersey.api.client.ClientResponse;

public abstract class DirEntry extends Connected {
	final Path2 handle;
	final String path;
	final Run run;

	protected DirEntry(Run run, String path) {
		this.run = run;
		this.path = path.replaceFirst("/+$", "");
		this.handle = run.run.wd().path2(this.path);
	}

	public void delete() throws ClientException, ServerException {
		checkError(handle.deleteAsXml(ClientResponse.class));
	}

	String path(ClientResponse response) throws ClientException, ServerException {
		checkError(response);
		String[] bits = response.getLocation().getPath().split("/");
		return concat(bits[bits.length - 1]);
	}

	String localName() {
		String[] bits = path.split("/");
		return bits[bits.length - 1];
	}

	String concat(String name) {
		return path + "/" + name.split("/", 2)[0];
	}
}