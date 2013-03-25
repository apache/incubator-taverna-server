/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.interaction;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.TavernaServerSupport;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.FilenameUtils;

/**
 * Bean that supports interaction feeds. This glues together the Abdera
 * serialization engine and the directory-based model used inside the server.
 * 
 * @author Donal Fellows
 */
public class InteractionFeedSupport {
	/**
	 * The name of the resource within the run resource that is the run's
	 * interaction feed resource.
	 */
	public static final String FEED_URL_DIR = "interaction";
	/**
	 * The name of the directory below the run working directory that will
	 * contain the entries of the interaction feed.
	 */
	public static final String FEED_DIR = "feed";
	/**
	 * Should the contents of the entry be stripped when describing the overall
	 * feed? This makes sense if (and only if) large entries are being pushed
	 * through the feed.
	 */
	private static final boolean STRIP_CONTENTS = false;
	/** Maximum size of an entry before truncation. */
	private static final long MAX_ENTRY_SIZE = 50 * 1024;

	private TavernaServerSupport support;
	private FilenameUtils utils;
	private Abdera abdera;
	private UriBuilderFactory uriBuilder;

	@Required
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Required
	public void setUtils(FilenameUtils utils) {
		this.utils = utils;
	}

	@Required
	public void setAbdera(Abdera abdera) {
		this.abdera = abdera;
	}

	@Required
	// webapp
	public void setUriBuilder(UriBuilderFactory uriBuilder) {
		this.uriBuilder = uriBuilder;
	}

	/**
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @return The URI of the feed
	 */
	public URI getFeedURI(TavernaRun run) {
		return uriBuilder.getRunUriBuilder(run).path(FEED_URL_DIR).build();
	}

	/**
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @param id
	 *            The ID of the entry.
	 * @return The URI of the entry.
	 */
	public URI getEntryURI(TavernaRun run, String id) {
		return uriBuilder.getRunUriBuilder(run)
				.path(FEED_URL_DIR + "/{entryID}").build(id);
	}

	private Entry getEntryFromFile(File f) throws FilesystemAccessException {
		long size = f.getSize();
		if (size > MAX_ENTRY_SIZE)
			throw new FilesystemAccessException("entry larger than 50kB");
		byte[] contents = f.getContents(0, (int) size);
		return (Entry) abdera.getParser()
				.parse(new ByteArrayInputStream(contents)).getRoot();
	}

	private void putEntryInFile(Directory dir, String name, Entry contents)
			throws FilesystemAccessException, NoUpdateException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			abdera.getWriter().writeTo(contents, baos);
		} catch (IOException e) {
			throw new NoUpdateException("failed to serialize the ATOM entry", e);
		}
		File f = dir.makeEmptyFile(support.getPrincipal(), name);
		f.appendContents(baos.toByteArray());
	}

	/**
	 * Get the interaction feed for a partciular run.
	 * 
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @return The Abdera feed descriptor.
	 * @throws FilesystemAccessException
	 *             If the feed directory can't be read for some reason.
	 * @throws NoDirectoryEntryException
	 *             If the feed directory doesn't exist or an entry is
	 *             unexpectedly removed.
	 */
	public Feed getRunFeed(TavernaRun run) throws FilesystemAccessException,
			NoDirectoryEntryException {
		Directory feedDir = utils.getDirectory(run, FEED_DIR);
		URI feedURI = getFeedURI(run);
		Feed feed = abdera.newFeed();
		feed.setTitle("Interactions for Taverna Run #" + run.getId());
		try {
			feed.addLink(feedURI.toURL().toString(), "self");
		} catch (MalformedURLException e) {
			// Should be impossible; ignore...
		}
		Date d = null;
		for (DirectoryEntry de : feedDir.getContentsByDate()) {
			if (!(de instanceof File))
				continue;
			try {
				Entry e = getEntryFromFile((File) de);
				if (STRIP_CONTENTS)
					e.setContentElement(null);
				feed.addEntry(e);
				d = de.getModificationDate();
			} catch (FilesystemAccessException e) {
				// Can't do anything about it, so we'll just drop the entry.
			}
		}
		if (d != null)
			feed.setUpdated(d);
		return feed;
	}

	/**
	 * Gets the contents of a particular feed entry.
	 * 
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @param entryID
	 *            The identifier (from the path) of the entry to read.
	 * @return The description of the entry.
	 * @throws FilesystemAccessException
	 *             If the entry can't be read or is too large.
	 * @throws NoDirectoryEntryException
	 *             If the entry can't be found.
	 */
	public Entry getRunFeedEntry(TavernaRun run, String entryID)
			throws FilesystemAccessException, NoDirectoryEntryException {
		File entryFile = utils
				.getFile(run, FEED_DIR + "/" + entryID + ".entry");
		return getEntryFromFile(entryFile);
	}

	/**
	 * Given a partial feed entry, store a complete feed entry in the filesystem
	 * for a particular run. Note that this does not permit update of an
	 * existing entry; the entry is always created new.
	 * 
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @param entry
	 *            The partial entry to store
	 * @return A link to the entry.
	 * @throws FilesystemAccessException
	 *             If the entry can't be stored.
	 * @throws NoDirectoryEntryException
	 *             If the run is improperly configured.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to do the write.
	 * @throws MalformedURLException
	 *             If a generated URL is illegal (shouldn't happen).
	 */
	public String addRunFeedEntry(TavernaRun run, Entry entry)
			throws FilesystemAccessException, NoDirectoryEntryException,
			NoUpdateException, MalformedURLException {
		support.permitUpdate(run);
		// TODO Should this id be generated like this?
		String localId = "interact_" + currentTimeMillis();
		entry.setId("urn:uuid:" + randomUUID());
		String selfLink = getEntryURI(run, localId).toURL().toString();
		entry.addLink(selfLink);
		entry.setUpdated(new Date());
		putEntryInFile(utils.getDirectory(run, FEED_DIR), localId + ".entry",
				entry);
		return selfLink;
	}

	/**
	 * Deletes an entry from a feed.
	 * 
	 * @param run
	 *            The workflow run that defines which feed we are operating on.
	 * @param entryID
	 *            The ID of the entry to delete.
	 * @throws FilesystemAccessException
	 *             If the entry can't be deleted
	 * @throws NoDirectoryEntryException
	 *             If the entry can't be found.
	 * @throws NoUpdateException
	 *             If the current user is not permitted to modify the run's
	 *             characteristics.
	 */
	public void removeRunFeedEntry(TavernaRun run, String entryID)
			throws FilesystemAccessException, NoDirectoryEntryException,
			NoUpdateException {
		support.permitUpdate(run);
		utils.getFile(run, FEED_DIR + "/" + entryID + ".entry").destroy();
	}
}
