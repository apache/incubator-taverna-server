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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.apache.taverna.server.client.TavernaServer.ClientException;
import org.apache.taverna.server.client.TavernaServer.ServerException;
import org.apache.taverna.server.client.generic.DirectoryEntry;
import org.apache.taverna.server.client.generic.DirectoryReference;
import org.apache.taverna.server.client.generic.FileReference;
import org.apache.taverna.server.client.rest.DirectoryContents;
import org.apache.taverna.server.client.rest.MakeDirectory;
import org.apache.taverna.server.client.rest.UploadFile;
import org.apache.taverna.server.client.wadl.TavernaServer.Root.RunsRunName.Wd;

import com.sun.jersey.api.client.ClientResponse;

public class Directory extends DirEntry {
	private final Wd wd;

	Directory(Run run) {
		super(run, "");
		this.wd = run.run.wd();
	}

	Directory(Run run, String path) {
		super(run, path);
		this.wd = run.run.wd();
	}

	public List<DirEntry> list() {
		List<DirEntry> result = new ArrayList<>();
		for (DirectoryEntry de : wd.path3(path)
				.getAsXml(DirectoryContents.class).getDirOrFile())
			if (de instanceof DirectoryReference)
				result.add(new Directory(run, de.getValue()));
			else if (de instanceof FileReference)
				result.add(new File(run, de.getValue()));
		return result;
	}

	public File createFile(String name, byte[] content) throws ClientException,
			ServerException {
		UploadFile uf = new UploadFile();
		uf.setName(name);
		uf.setValue(content);
		return new File(run, path(wd.path(path).putAsXml(uf,
				ClientResponse.class)));
	}

	public File createFile(String name, java.io.File content)
			throws ClientException, ServerException {
		return new File(run, path(wd.path(concat(name)).putOctetStreamAsXml(
				entity(content, APPLICATION_OCTET_STREAM_TYPE),
				ClientResponse.class)));
	}

	public File createFile(String name, URI source) throws ClientException,
			ServerException {
		return new File(run, path(wd.path(concat(name)).postTextUriListAsXml(
				source.toString(), ClientResponse.class)));
	}

	public Directory createDirectory(String name) throws ClientException,
			ServerException {
		MakeDirectory mkdir = new MakeDirectory();
		mkdir.setName(name);
		return new Directory(run, path(wd.path(path).putAsXml(mkdir,
				ClientResponse.class)));
	}

	public byte[] getZippedContents() {
		return wd.path3(path).getAsZip(byte[].class);
	}

	public ZipFile getZip() throws IOException {
		byte[] contents = getZippedContents();
		java.io.File tmp = createTempFile(localName(), ".zip");
		try (OutputStream os = new FileOutputStream(tmp)) {
			os.write(contents);
		}
		return new ZipFile(tmp);
	}
}