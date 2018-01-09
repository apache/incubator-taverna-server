/*
 */
package org.apache.taverna.server.master.common;
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

import static org.apache.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.taverna.server.master.interfaces.Directory;
import org.apache.taverna.server.master.interfaces.DirectoryEntry;

/**
 * A reference to something that is in a directory below the working directory
 * of a workflow run, described using JAXB. Note that when creating an XML
 * document containing one of these in a client, it is <i>not</i> necessary to
 * supply any attribute.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "DirectoryEntry")
@XmlSeeAlso( { DirEntryReference.DirectoryReference.class,
		DirEntryReference.FileReference.class })
public class DirEntryReference {
	/** A link to the entry. Ignored on input. */
	@XmlAttribute(name = "href", namespace = XLINK)
	@XmlSchemaType(name = "anyURI")
	public URI link;
	/** The last, user-displayable part of the name. Ignored on input. */
	@XmlAttribute
	public String name;
	/** The path of the entry. */
	@XmlValue
	public String path;

	/**
	 * Return the directory entry reference instance subclass suitable for the
	 * given directory entry.
	 * 
	 * @param entry
	 *            The entry to characterise.
	 * @return An object that describes the directory entry.
	 */
	public static DirEntryReference newInstance(DirectoryEntry entry) {
		return newInstance(null, entry);
	}

	/**
	 * Return the directory entry reference instance subclass suitable for the
	 * given directory entry.
	 * 
	 * @param ub
	 *            Used for constructing URIs. The {@link #link} field is not
	 *            filled in if this is <tt>null</tt>.
	 * @param entry
	 *            The entry to characterise.
	 * @return An object that describes the directory entry.
	 */
	// Really returns a subclass, so cannot be constructor
	public static DirEntryReference newInstance(UriBuilder ub,
			DirectoryEntry entry) {
		DirEntryReference de = (entry instanceof Directory) ? new DirectoryReference()
				: new FileReference();
		de.name = entry.getName();
		String fullname = entry.getFullName();
		de.path = fullname.startsWith("/") ? fullname.substring(1) : fullname;
		if (ub != null)
			de.link = ub.build(entry.getName());
		return de;
	}

	/** A reference to a directory, done with JAXB. */
	@XmlRootElement(name = "dir")
	@XmlType(name = "DirectoryReference")
	public static class DirectoryReference extends DirEntryReference {
	}

	/** A reference to a file, done with JAXB. */
	@XmlRootElement(name = "file")
	@XmlType(name = "FileReference")
	public static class FileReference extends DirEntryReference {
	}
}
