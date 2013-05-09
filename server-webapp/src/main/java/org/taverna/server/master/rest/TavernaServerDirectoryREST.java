/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static java.util.Collections.unmodifiableList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.taverna.server.master.common.Roles.USER;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.File;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Representation of how a workflow run's working directory tree looks.
 * 
 * @author Donal Fellows
 */
@RolesAllowed(USER)
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
@Description("Representation of how a workflow run's working directory tree looks.")
public interface TavernaServerDirectoryREST {
	/**
	 * Get the working directory of the workflow run.
	 * 
	 * @param ui
	 *            About how this method was called.
	 * @return A description of the working directory.
	 * @throws FilesystemAccessException
	 */
	@GET
	@Path("/")
	@Description("Describes the working directory of the workflow run.")
	@NonNull
	DirectoryContents getDescription(@NonNull @Context UriInfo ui)
			throws FilesystemAccessException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("{path:.*}")
	@Description("Produces the description of the files/directories' baclava operations.")
	Response options(@PathParam("path") List<PathSegment> path);

	/**
	 * Gets a description of the named entity in or beneath the working
	 * directory of the workflow run, which may be either a {@link Directory} or
	 * a {@link File}.
	 * 
	 * @param path
	 *            The path to the thing to describe.
	 * @param ui
	 *            About how this method was called.
	 * @param headers
	 *            About what the caller was looking for.
	 * @return An HTTP response containing a description of the named thing.
	 * @throws NoDirectoryEntryException
	 *             If the name of the file or directory can't be looked up.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 * @throws NegotiationFailedException
	 *             If the content type being downloaded isn't one that this
	 *             method can support.
	 */
	@GET
	@Path("{path:.+}")
	@Produces({ "application/xml", "application/json",
			"application/octet-stream", "application/zip", "*/*" })
	@Description("Gives a description of the named entity in or beneath the working directory of the workflow run (either a Directory or File).")
	@NonNull
	Response getDirectoryOrFileContents(
			@NonNull @PathParam("path") List<PathSegment> path,
			@NonNull @Context UriInfo ui, @NonNull @Context HttpHeaders headers)
			throws NoDirectoryEntryException, FilesystemAccessException,
			NegotiationFailedException;

	/**
	 * Creates a directory in the filesystem beneath the working directory of
	 * the workflow run, or creates or updates a file's contents, where that
	 * file is in or below the working directory of a workflow run.
	 * 
	 * @param parent
	 *            The directory to create the directory in.
	 * @param operation
	 *            What to call the directory to create.
	 * @param ui
	 *            About how this method was called.
	 * @return An HTTP response indicating where the directory was actually made
	 *         or what file was created/updated.
	 * @throws NoDirectoryEntryException
	 *             If the name of the containing directory can't be looked up.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@POST
	@Path("{path:.*}")
	@Description("Creates a directory in the filesystem beneath the working directory of the workflow run, or creates or updates a file's contents, where that file is in or below the working directory of a workflow run.")
	@NonNull
	Response makeDirectoryOrUpdateFile(
			@NonNull @PathParam("path") List<PathSegment> parent,
			@NonNull MakeOrUpdateDirEntry operation,
			@NonNull @Context UriInfo ui) throws NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException;

	/**
	 * Creates or updates a file in a particular location beneath the working
	 * directory of the workflow run.
	 * 
	 * @param file
	 *            The path to the file to create or update.
	 * @param referenceList
	 *            Location to get the file's contents from. Must be
	 *            <i>publicly</i> readable.
	 * @param ui
	 *            About how this method was called.
	 * @return An HTTP response indicating what file was created/updated.
	 * @throws NoDirectoryEntryException
	 *             If the name of the containing directory can't be looked up.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@POST
	@Path("{path:(.*)}")
	@Consumes("text/uri-list")
	@Description("Creates or updates a file in a particular location beneath the working directory of the workflow run with the contents of a publicly readable URL.")
	@NonNull
	Response setFileContentsFromURL(@PathParam("path") List<PathSegment> file,
			List<URI> referenceList, @Context UriInfo ui)
			throws NoDirectoryEntryException, NoUpdateException,
			FilesystemAccessException;

    /**
	 * Creates or updates a file in a particular location beneath the working
	 * directory of the workflow run.
	 * 
	 * @param file
	 *            The path to the file to create or update.
	 * @param contents
	 *            Stream of bytes to set the file's contents to.
	 * @param ui
	 *            About how this method was called.
	 * @return An HTTP response indicating what file was created/updated.
	 * @throws NoDirectoryEntryException
	 *             If the name of the containing directory can't be looked up.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@PUT
	@Path("{path:(.*)}")
	@Consumes(APPLICATION_OCTET_STREAM)
	@Description("Creates or updates a file in a particular location beneath the working directory of the workflow run.")
	@NonNull
	Response setFileContents(@PathParam("path") List<PathSegment> file,
			InputStream contents, @Context UriInfo ui)
			throws NoDirectoryEntryException, NoUpdateException,
			FilesystemAccessException;

	/**
	 * Deletes a file or directory that is in or below the working directory of
	 * a workflow run.
	 * 
	 * @param path
	 *            The path to the file or directory.
	 * @return An HTTP response to the method.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 * @throws NoDirectoryEntryException
	 *             If the name of the file or directory can't be looked up.
	 */
	@DELETE
	@Path("{path:.*}")
	@Description("Deletes a file or directory that is in or below the working directory of a workflow run.")
	@NonNull
	Response destroyDirectoryEntry(@PathParam("path") List<PathSegment> path)
			throws NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException;

	/**
	 * Exception thrown to indicate a failure by the client to provide an
	 * acceptable content type.
	 * 
	 * @author Donal Fellows
	 */
	@SuppressWarnings("serial")
	public static class NegotiationFailedException extends Exception {
		public List<Variant> accepted;

		public NegotiationFailedException(String msg, List<Variant> accepted) {
			super(msg);
			this.accepted = unmodifiableList(accepted);
		}
	}
}
