package org.taverna.server.master.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.Description;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.File;

/**
 * Representation of how a workflow run's working directory tree looks.
 * 
 * @author Donal Fellows
 */
@Produces( { "application/xml", "application/json" })
@Consumes( { "application/xml", "application/json" })
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
	public DirectoryContents getDescription(@Context UriInfo ui)
			throws FilesystemAccessException;

	/**
	 * Gets a description of the named entity in or beneath the working
	 * directory of the workflow run, which may be either a {@link Directory} or
	 * a {@link File}.
	 * 
	 * @param path
	 *            The path to the thing to describe.
	 * @param ui
	 *            About how this method was called.
	 * @param req
	 *            About what the caller was looking for.
	 * @return An HTTP response containing a description of the named thing.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@GET
	@Path("{path:.+}")
	@Produces( { "application/xml", "application/json",
			"application/octet-stream", "application/zip" })
	@Description("Gives a description of the named entity in or beneath the working directory of the workflow run (either a Directory or File).")
	public Response getDirectoryOrFileContents(
			@PathParam("path") List<PathSegment> path, @Context UriInfo ui,
			@Context Request req) throws FilesystemAccessException;

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
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@POST
	@Path("{path:.*}")
	@Description("Creates a directory in the filesystem beneath the working directory of the workflow run, or creates or updates a file's contents, where that file is in or below the working directory of a workflow run.")
	public Response makeDirectoryOrUpdateFile(
			@PathParam("path") List<PathSegment> parent,
			MakeOrUpdateDirEntry operation, @Context UriInfo ui)
			throws NoUpdateException, FilesystemAccessException;

	/**
	 * Deletes a file or directory that is in or below the working directory of
	 * a workflow run.
	 * 
	 * @param path
	 *            The path to the file or directory.
	 * @param ui
	 *            About how this method was called.
	 * @return An HTTP response to the method.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If something went wrong during the filesystem operation.
	 */
	@DELETE
	@Path("{path:.*}")
	@Description("Deletes a file or directory that is in or below the working directory of a workflow run.")
	public Response destroyDirectoryEntry(
			@PathParam("path") List<PathSegment> path, @Context UriInfo ui)
			throws NoUpdateException, FilesystemAccessException;
}
