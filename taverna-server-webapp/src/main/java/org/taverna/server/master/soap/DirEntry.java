/*
 */
package org.taverna.server.master.soap;
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

import static org.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.taverna.server.master.common.DirEntryReference;

/**
 * A more Taverna-friendly version of the directory entry descriptor classes.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "DirectoryEntry")
@XmlRootElement(name = "entry")
@XmlSeeAlso({ DirEntry.File.class, DirEntry.Directory.class })
public class DirEntry {
	/** A link to the entry. Ignored on input. */
	@XmlAttribute(name = "href", namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI link;
	@XmlAttribute
	public String name;
	@XmlElement(required = true)
	public String path;

	/**
	 * A file in a directory.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "FileDirEntry")
	@XmlRootElement(name = "file")
	public static class File extends DirEntry {
	}

	/**
	 * A directory in a directory. That is, a sub-directory.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "DirectoryDirEntry")
	@XmlRootElement(name = "dir")
	public static class Directory extends DirEntry {
	}

	/**
	 * Converts from the "common" format to the subclasses of this class.
	 * 
	 * @param deref
	 *            The "common" format handle to convert.
	 * @return The converted handle
	 */
	public static DirEntry convert(DirEntryReference deref) {
		DirEntry result;
		if (deref instanceof DirEntryReference.DirectoryReference)
			result = new Directory();
		else if (deref instanceof DirEntryReference.FileReference)
			result = new File();
		else
			result = new DirEntry();
		result.link = deref.link;
		result.name = deref.name;
		result.path = deref.path;
		return result;
	}

	/**
	 * Converts to the "common" format from the subclasses of this class.
	 * 
	 * @param deref
	 *            The subclass of this class to convert.
	 * @return The converted reference.
	 */
	public static DirEntryReference convert(DirEntry de) {
		DirEntryReference result;
		if (de instanceof Directory)
			result = new DirEntryReference.DirectoryReference();
		else if (de instanceof File)
			result = new DirEntryReference.FileReference();
		else
			result = new DirEntryReference();
		result.link = de.link;
		result.name = de.name;
		result.path = de.path;
		return result;
	}
}
