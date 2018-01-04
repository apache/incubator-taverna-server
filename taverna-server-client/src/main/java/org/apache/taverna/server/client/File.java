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

import static java.io.File.createTempFile;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.tika.mime.MimeTypes.getDefaultMimeTypes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.taverna.server.client.TavernaServer.ClientException;
import org.apache.taverna.server.client.TavernaServer.ServerException;
import org.apache.taverna.server.client.wadl.TavernaServer.Root.RunsRunName.Wd;
import org.apache.tika.mime.MimeTypeException;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public class File extends DirEntry {
	private final Wd wd;

	File(Run run, String path) {
		super(run, path);
		wd = run.run.wd();
	}

	public InputStream getAsStream() {
		return wd.path3(path).getAsOctetStream(InputStream.class);
	}

	public byte[] get() {
		return wd.path3(path).getAsOctetStream(byte[].class);
	}

	public String get(Charset encoding) {
		return new String(wd.path3(path).getAsOctetStream(byte[].class),
				encoding);
	}

	public java.io.File getAsFile() throws ClientHandlerException,
			UniformInterfaceException, IOException, MimeTypeException,
			ClientException, ServerException {
		ClientResponse cr = wd.path3(path).getAsOctetStream(
				ClientResponse.class);
		checkError(cr);
		String[] bits = localName().split("[.]");
		String ext = getDefaultMimeTypes().forName(
				cr.getHeaders().getFirst("Content-Type")).getExtension();
		if (ext == null)
			ext = bits[bits.length - 1];
		java.io.File tmp = createTempFile(bits[0], ext);
		try (OutputStream os = new FileOutputStream(tmp);
				InputStream is = cr.getEntity(InputStream.class)) {
			copy(is, os);
		}
		return tmp;
	}

	public void setContents(byte[] newContents) throws ClientException,
			ServerException {
		checkError(wd.path(path).putOctetStreamAsXml(newContents,
				ClientResponse.class));
	}

	public void setContents(String newContents) throws ClientException,
			ServerException {
		checkError(wd.path(path).putOctetStreamAsXml(newContents,
				ClientResponse.class));
	}

	public void setContents(String newContents, Charset encoding)
			throws ClientException, ServerException {
		checkError(wd.path(path).putOctetStreamAsXml(
				newContents.getBytes(encoding), ClientResponse.class));
	}

	public void setContents(InputStream newContents) throws ClientException,
			ServerException {
		checkError(wd.path(path).putOctetStreamAsXml(newContents,
				ClientResponse.class));
	}

	public void setContents(java.io.File newContents) throws IOException,
			ClientException, ServerException {
		checkError(wd.path(path).putOctetStreamAsXml(
				entity(newContents, APPLICATION_OCTET_STREAM_TYPE),
				ClientResponse.class));
	}
}