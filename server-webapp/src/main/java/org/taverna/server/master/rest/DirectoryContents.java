/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Uri.secure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.taverna.server.master.common.DirEntryReference;
import org.taverna.server.master.interfaces.DirectoryEntry;

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
		contents = new ArrayList<DirEntryReference>();
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
		contents = new ArrayList<DirEntryReference>();
		UriBuilder ub = secure(ui).path("{path}");
		for (DirectoryEntry e : collection) {
			contents.add(DirEntryReference.newInstance(ub, e));
		}
	}
}
