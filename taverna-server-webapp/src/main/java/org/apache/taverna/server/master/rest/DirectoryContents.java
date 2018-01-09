/*
 */
package org.apache.taverna.server.master.rest;
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

import static org.apache.taverna.server.master.common.Uri.secure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.apache.taverna.server.master.common.DirEntryReference;
import org.apache.taverna.server.master.interfaces.DirectoryEntry;

/**
 * The result of a RESTful operation to list the contents of a directory. Done
 * with JAXB.
 * 
 * @author Donal Fellows
 */
@XmlRootElement
@XmlType(name = "DirectoryContents")
@XmlSeeAlso(MakeOrUpdateDirEntry.class)
public class DirectoryContents {
	/**
	 * The contents of the directory.
	 */
	@XmlElementRef
	public List<DirEntryReference> contents;

	/**
	 * Make an empty directory description. Required for JAXB.
	 */
	public DirectoryContents() {
		contents = new ArrayList<>();
	}

	/**
	 * Make a directory description.
	 * 
	 * @param ui
	 *            The factory for URIs.
	 * @param collection
	 *            The real directory contents that we are to describe.
	 */
	public DirectoryContents(UriInfo ui, Collection<DirectoryEntry> collection) {
		contents = new ArrayList<>();
		UriBuilder ub = secure(ui).path("{filename}");
		for (DirectoryEntry e : collection)
			contents.add(DirEntryReference.newInstance(ub, e));
	}
}
