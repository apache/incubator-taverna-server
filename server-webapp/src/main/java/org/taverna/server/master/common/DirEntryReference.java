/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import static org.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;

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
