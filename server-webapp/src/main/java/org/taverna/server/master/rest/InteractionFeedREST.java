/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import java.net.MalformedURLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;

/**
 * A very stripped down ATOM feed for the interaction service.
 * 
 * @author Donal Fellows
 */
public interface InteractionFeedREST {
	/**
	 * Get the feed document for this ATOM feed.
	 * 
	 * @return The feed.
	 * @throws FilesystemAccessException
	 *             If we can't read from the feed directory.
	 * @throws NoDirectoryEntryException
	 *             If something changes things under our feet.
	 */
	@GET
	@Path("/")
	@Produces("application/atom+xml")
	@Description("Get the feed document for this ATOM feed.")
	Feed getFeed() throws FilesystemAccessException, NoDirectoryEntryException;

	/**
	 * Adds an entry to this ATOM feed.
	 * 
	 * @param entry
	 *            The entry to create.
	 * @return A redirect to the created entry.
	 * @throws MalformedURLException
	 *             If we have problems generating the URI of the entry.
	 * @throws FilesystemAccessException
	 *             If we can't create the feed entry file.
	 * @throws NoDirectoryEntryException
	 *             If things get changed under our feet.
	 * @throws NoUpdateException
	 *             If we don't have permission to change things relating to this
	 *             run.
	 */
	@POST
	@Path("/")
	@Consumes("application/atom+xml")
	@Description("Adds an entry to this ATOM feed.")
	Response addEntry(Entry entry) throws MalformedURLException,
			FilesystemAccessException, NoDirectoryEntryException,
			NoUpdateException;

	/** Handles the OPTIONS request. */
	@OPTIONS
	@Path("/")
	@Description("Describes what HTTP operations are supported on the feed.")
	Response feedOptions();

	/**
	 * Gets the content of an entry in this ATOM feed.
	 * 
	 * @param id
	 *            The ID of the entry to fetch.
	 * @return The entry contents.
	 * @throws FilesystemAccessException
	 *             If we have problems reading the entry.
	 * @throws NoDirectoryEntryException
	 *             If we can't find the entry to read.
	 */
	@GET
	@Path("{id}")
	@Produces("application/atom+xml")
	@Description("Get the entry with a particular ID within this ATOM feed.")
	Entry getEntry(@PathParam("id") String id)
			throws FilesystemAccessException, NoDirectoryEntryException;

	/**
	 * Delete an entry from this ATOM feed.
	 * 
	 * @param id
	 *            The ID of the entry to delete.
	 * @return A simple message. Not very important!
	 * @throws FilesystemAccessException
	 *             If we have problems deleting the entry.
	 * @throws NoDirectoryEntryException
	 *             If we can't find the entry to delete.
	 * @throws NoUpdateException
	 *             If we don't have permission to alter things relating to this
	 *             run.
	 */
	@DELETE
	@Path("{id}")
	@Produces("text/plain")
	@Description("Deletes an entry from this ATOM feed.")
	String deleteEntry(@PathParam("id") String id)
			throws FilesystemAccessException, NoDirectoryEntryException,
			NoUpdateException;

	/** Handles the OPTIONS request. */
	@OPTIONS
	@Path("{id}")
	@Description("Describes what HTTP operations are supported on an entry.")
	Response entryOptions(@PathParam("{id}") String id);
}
