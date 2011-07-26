/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.seeOther;
import static org.taverna.server.master.ContentTypes.APPLICATION_ZIP_TYPE;
import static org.taverna.server.master.ContentTypes.DIRECTORY_VARIANTS;
import static org.taverna.server.master.ContentTypes.INITIAL_FILE_VARIANTS;
import static org.taverna.server.master.TavernaServerImpl.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerImpl.SupportAware;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.DirectoryContents;
import org.taverna.server.master.rest.FileSegment;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry.MakeDirectory;
import org.taverna.server.master.rest.TavernaServerDirectoryREST;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * RESTful access to the filesystem.
 * 
 * @author Donal Fellows
 */
class DirectoryREST implements TavernaServerDirectoryREST, DirectoryBean {
	private TavernaServerSupport support;
	private TavernaRun run;
	private FilenameUtils fileUtils;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	@Required
	public void setFileUtils(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	@Override
	public DirectoryREST connect(TavernaRun run) {
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	public Response destroyDirectoryEntry(List<PathSegment> path)
			throws NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		support.permitUpdate(run);
		fileUtils.getDirEntry(run, path).destroy();
		return noContent().build();
	}

	@Override
	@CallCounted
	public DirectoryContents getDescription(UriInfo ui)
			throws FilesystemAccessException {
		return new DirectoryContents(ui, run.getWorkingDirectory()
				.getContents());
	}

	// Nasty! This can have several different responses...
	// @Override
	@CallCounted
	public Response getDirectoryOrFileContents(List<PathSegment> path,
			UriInfo ui, Request req) throws FilesystemAccessException,
			NoDirectoryEntryException {
		DirectoryEntry de = fileUtils.getDirEntry(run, path);

		// How did the user want the result?
		List<Variant> variants = getVariants(de);
		Variant v = req.selectVariant(variants);
		if (v == null)
			return notAcceptable(variants).type(TEXT_PLAIN)
					.entity("Do not know what type of response to produce.")
					.build();

		// Produce the content to deliver up
		Object result;
		if (v.getMediaType().equals(APPLICATION_OCTET_STREAM_TYPE))
			// Only for files...
			result = de;
		else if (v.getMediaType().equals(APPLICATION_ZIP_TYPE))
			// Only for directories...
			result = ((Directory) de).getContentsAsZip();
		else
			// Only for directories...
			// XML or JSON; let CXF pick what to do
			result = new DirectoryContents(ui, ((Directory) de).getContents());
		return ok(result).type(v.getMediaType()).build();
	}

	private boolean matchType(MediaType a, MediaType b) {
		log.debug("comparing " + a.getType() + "/" + a.getSubtype() + " and "
				+ b.getType() + "/" + b.getSubtype());
		return (a.isWildcardType() || b.isWildcardType() || a.getType().equals(
				b.getType()))
				&& (a.isWildcardSubtype() || b.isWildcardSubtype() || a
						.getSubtype().equals(b.getSubtype()));
	}

	/**
	 * What are we willing to serve up a directory or file as?
	 * 
	 * @param de
	 *            The reference to the object to serve.
	 * @return The variants we can serve it as.
	 * @throws FilesystemAccessException
	 *             If we fail to read data necessary to detection of its media
	 *             type.
	 */
	private List<Variant> getVariants(DirectoryEntry de)
			throws FilesystemAccessException {
		if (de instanceof Directory)
			return DIRECTORY_VARIANTS;
		else if (!(de instanceof File))
			throw new FilesystemAccessException("not a directory or file!");
		File f = (File) de;
		List<Variant> variants = new ArrayList<Variant>(INITIAL_FILE_VARIANTS);
		String contentType = support.getEstimatedContentType(f);
		if (!contentType.equals(APPLICATION_OCTET_STREAM)) {
			String[] ct = contentType.split("/");
			variants.add(0,
					new Variant(new MediaType(ct[0], ct[1]), null, null));
		}
		return variants;
	}

	@Override
	@CallCounted
	public Response getDirectoryOrFileContents(List<PathSegment> path,
			UriInfo ui, HttpHeaders headers) throws FilesystemAccessException,
			NoDirectoryEntryException {
		DirectoryEntry de = fileUtils.getDirEntry(run, path);

		// How did the user want the result?
		List<Variant> variants = getVariants(de);
		MediaType wanted = null;
		// Manual content negotiation!!! Ugh!
		outer: for (MediaType mt : headers.getAcceptableMediaTypes()) {
			for (Variant v : variants) {
				if (matchType(mt, v.getMediaType())) {
					wanted = v.getMediaType();
					break outer;
				}
			}
		}
		if (wanted == null)
			return notAcceptable(variants).type(TEXT_PLAIN)
					.entity("Do not know what type of response to produce.")
					.build();

		log.info("producing content of type " + wanted);
		// Produce the content to deliver up
		Object result;
		if (de instanceof File) {
			// Only for files...
			result = de;
			List<String> range = headers.getRequestHeader("Range");
			if (range != null && range.size() == 1) {
				return new FileSegment((File) de, range.get(0)).toResponse(wanted);
			}
		} else {
			// Only for directories...
			Directory d = (Directory) de;
			if (wanted.getType().equals(APPLICATION_ZIP_TYPE.getType())
					&& wanted.getSubtype().equals(
							APPLICATION_ZIP_TYPE.getSubtype()))
				result = d.getContentsAsZip();
			else
				// XML or JSON; let CXF pick what to do
				result = new DirectoryContents(ui, d.getContents());
		}
		return ok(result).type(wanted).build();
	}

	@Override
	@CallCounted
	public Response makeDirectoryOrUpdateFile(List<PathSegment> parent,
			MakeOrUpdateDirEntry op, UriInfo ui) throws NoUpdateException,
			FilesystemAccessException, NoDirectoryEntryException {
		support.permitUpdate(run);
		DirectoryEntry container = fileUtils.getDirEntry(run, parent);
		if (!(container instanceof Directory))
			throw new FilesystemAccessException("You may not "
					+ ((op instanceof MakeDirectory) ? "make a subdirectory of"
							: "place a file in") + " a file.");
		if (op.name == null || op.name.length() == 0)
			throw new FilesystemAccessException("missing name attribute");
		Directory d = (Directory) container;
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{name}");

		// Make a directory in the context directory

		if (op instanceof MakeDirectory) {
			Directory target = d.makeSubdirectory(support.getPrincipal(),
					op.name);
			return created(ub.build(target.getName())).build();
		}

		// Make or set the contents of a file

		File f = null;
		for (DirectoryEntry e : d.getContents()) {
			if (e.getName().equals(op.name)) {
				if (e instanceof Directory)
					throw new FilesystemAccessException(
							"You may not overwrite a directory with a file.");
				f = (File) e;
				break;
			}
		}
		if (f == null) {
			f = d.makeEmptyFile(support.getPrincipal(), op.name);
			f.setContents(op.contents);
			return created(ub.build(f.getName())).build();
		}
		f.setContents(op.contents);
		return seeOther(ub.build(f.getName())).build();
	}

	@Override
	@CallCounted
	public Response setFileContents(List<PathSegment> filePath, String name,
			InputStream contents, UriInfo ui) throws NoDirectoryEntryException,
			NoUpdateException, FilesystemAccessException {
		support.permitUpdate(run);
		Directory d;
		if (filePath != null && filePath.size() > 0) {
			DirectoryEntry e = fileUtils.getDirEntry(run, filePath);
			if (!(e instanceof Directory)) {
				throw new FilesystemAccessException(
						"Cannot create a file that is not in a directory.");
			}
			d = (Directory) e;
		} else {
			d = run.getWorkingDirectory();
		}

		File f = null;
		for (DirectoryEntry e : d.getContents()) {
			if (e.getName().equals(name)) {
				if (e instanceof File) {
					f = (File) e;
					break;
				}
				throw new FilesystemAccessException(
						"Cannot create a file that is not in a directory.");
			}
		}
		if (f == null)
			f = d.makeEmptyFile(support.getPrincipal(), name);

		try {
			byte[] buffer = new byte[65536];
			int len = contents.read(buffer);
			if (len >= 0) {
				if (len < buffer.length) {
					byte[] newBuf = new byte[len];
					System.arraycopy(buffer, 0, newBuf, 0, len);
					buffer = newBuf;
				}
				f.setContents(buffer);
				while (len == 65536) {
					len = contents.read(buffer);
					if (len < 1)
						break;
					if (len < buffer.length) {
						byte[] newBuf = new byte[len];
						System.arraycopy(buffer, 0, newBuf, 0, len);
						buffer = newBuf;
					}
					f.appendContents(buffer);
				}
			}
		} catch (IOException exn) {
			throw new FilesystemAccessException("failed to transfer bytes", exn);
		}
		return seeOther(ui.getAbsolutePath()).build();
	}
}

/**
 * Description of properties supported by {@link DirectoryREST}.
 * 
 * @author Donal Fellows
 */
interface DirectoryBean extends SupportAware {
	void setFileUtils(FilenameUtils fileUtils);

	DirectoryREST connect(TavernaRun run);
}