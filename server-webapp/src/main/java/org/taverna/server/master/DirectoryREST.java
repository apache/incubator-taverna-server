/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static eu.medsea.util.MimeUtil.getMimeType;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notAcceptable;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.seeOther;
import static org.taverna.server.master.TavernaServerImpl.log;

import java.io.ByteArrayInputStream;
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
import org.taverna.server.master.rest.MakeOrUpdateDirEntry;
import org.taverna.server.master.rest.MakeOrUpdateDirEntry.MakeDirectory;
import org.taverna.server.master.rest.TavernaServerDirectoryREST;
import org.taverna.server.master.utils.FilenameUtils;

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
	public Response destroyDirectoryEntry(List<PathSegment> path)
			throws NoUpdateException, FilesystemAccessException,
			NoDirectoryEntryException {
		support.permitUpdate(run);
		fileUtils.getDirEntry(run, path).destroy();
		return noContent().build();
	}

	@Override
	public DirectoryContents getDescription(UriInfo ui)
			throws FilesystemAccessException {
		return new DirectoryContents(ui, run.getWorkingDirectory()
				.getContents());
	}

	// Nasty! This can have several different responses...
	// @Override
	public Response getDirectoryOrFileContents(List<PathSegment> path,
			UriInfo ui, Request req) throws FilesystemAccessException,
			NoDirectoryEntryException {
		DirectoryEntry de = fileUtils.getDirEntry(run, path);

		// How did the user want the result?
		List<Variant> variants;
		if (de instanceof File)
			variants = fileVariants;
		else if (de instanceof Directory)
			variants = directoryVariants;
		else
			throw new FilesystemAccessException("not a directory or file!");
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

	/** "application/zip" */
	static final MediaType APPLICATION_ZIP_TYPE = new MediaType("application",
			"zip");
	static final List<Variant> directoryVariants = asList(new Variant(
			APPLICATION_XML_TYPE, null, null), new Variant(
			APPLICATION_JSON_TYPE, null, null), new Variant(
			APPLICATION_ZIP_TYPE, null, null));
	static final List<Variant> fileVariants = singletonList(new Variant(
			APPLICATION_OCTET_STREAM_TYPE, null, null));
	/** "application/vnd.taverna.baclava+xml" */
	private static final MediaType BACLAVA_MEDIA_TYPE = new MediaType(
			"application", "vnd.taverna.baclava+xml");

	@Override
	public Response getDirectoryOrFileContents(List<PathSegment> path,
			UriInfo ui, HttpHeaders headers) throws FilesystemAccessException,
			NoDirectoryEntryException {
		DirectoryEntry de = fileUtils.getDirEntry(run, path);

		// How did the user want the result?
		List<Variant> variants;
		if (de instanceof File) {
			variants = new ArrayList<Variant>(fileVariants);
			if (de.getName().endsWith(".baclava"))
				variants.add(0, new Variant(BACLAVA_MEDIA_TYPE, null, null));
			else
				try {
					File f = (File) de;
					byte[] head = f.getContents(0, 1024);
					String contentType = getMimeType(new ByteArrayInputStream(
							head));
					String[] ct = contentType.split("/");
					if (!contentType.equals(APPLICATION_OCTET_STREAM))
						variants.add(0, new Variant(
								new MediaType(ct[0], ct[1]), null, null));
				} catch (FilesystemAccessException e) {
					// Ignore; fall back to just serving as bytes
				}
		} else if (de instanceof Directory)
			variants = directoryVariants;
		else
			throw new FilesystemAccessException("not a directory or file!");
		MediaType wanted = null;
		log.info("wanted this " + headers.getAcceptableMediaTypes());
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

		// Produce the content to deliver up
		Object result;
		if (de instanceof File) {
			// Only for files...
			result = de;
			if (wanted.getType().equals("text")) {
				File f = (File) de;
				result = new String(f.getContents(0, (int) f.getSize()),
						defaultCharset());
				// Explicitly assumed that the system's charset is correct
			}
		} else if (wanted.equals(APPLICATION_ZIP_TYPE))
			// Only for directories...
			result = ((Directory) de).getContentsAsZip();
		else
			// Only for directories...
			// XML or JSON; let CXF pick what to do
			result = new DirectoryContents(ui, ((Directory) de).getContents());
		return ok(result).type(wanted).build();
	}

	@Override
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
 * @author Donal Fellows
 */
interface DirectoryBean extends SupportAware {
	void setFileUtils(FilenameUtils fileUtils);
	DirectoryREST connect(TavernaRun run);
}