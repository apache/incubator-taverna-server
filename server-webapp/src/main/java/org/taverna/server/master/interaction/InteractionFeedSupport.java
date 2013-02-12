package org.taverna.server.master.interaction;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.taverna.server.master.ContentsDescriptorBuilder.UriBuilderFactory;
import org.taverna.server.master.TavernaServerSupport;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.FilenameUtils;

public class InteractionFeedSupport {
	private TavernaServerSupport support;
	private FilenameUtils utils;
	private Abdera abdera;
	private UriBuilderFactory uriBuilder;

	private Entry getEntryFromFile(File f) throws FilesystemAccessException {
		byte[] contents = f.getContents(0, (int) f.getSize());
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

	public Feed getRunFeed(String runID) throws FilesystemAccessException,
			NoDirectoryEntryException, UnknownRunException,
			MalformedURLException {
		TavernaRun run = support.getRun(runID);
		Directory feedDir = utils.getDirectory(run, "feed");
		URI feedURI = uriBuilder.getRunUriBuilder(run).path("feed").build();
		Feed feed = abdera.newFeed();
		feed.setTitle("Interactions for Taverna Run #" + runID);
		feed.addLink(feedURI.toURL().toString(), "self");
		Date d = null;
		for (DirectoryEntry de : feedDir.getContentsByDate()) {
			if (!(de instanceof File))
				continue;
			Entry e = getEntryFromFile((File) de);
			// TODO Should the contents of the entry be stripped?
			//e.setContentElement(null);
			feed.addEntry(e);
			d = de.getModificationDate();
		}
		feed.setUpdated(d);
		return feed;
	}

	/**
	 * Gets the contents of a particular feed entry.
	 * @param runID
	 * @param entryID
	 * @return
	 * @throws FilesystemAccessException
	 * @throws NoDirectoryEntryException
	 * @throws UnknownRunException
	 */
	public Entry getRunFeedEntry(String runID, String entryID)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException {
		File entryFile = utils.getFile(support.getRun(runID), "feed/" + entryID
				+ ".entry");
		return getEntryFromFile(entryFile);
	}

	/**
	 * Given a partial feed entry, store a complete feed entry in the filesystem
	 * for a particular run. Note that this does not permit update of an
	 * existing entry; the entry is always created new.
	 * 
	 * @param runID
	 *            The ID of the run.
	 * @param entry
	 *            The partial entry to store
	 * @return A link to the entry.
	 * @throws FilesystemAccessException
	 *             If the entry can't be stored.
	 * @throws NoDirectoryEntryException
	 *             If the run is improperly configured.
	 * @throws UnknownRunException
	 *             If the run is unknown.
	 * @throws NoUpdateException
	 *             If the user isn't allowed to do the write.
	 * @throws MalformedURLException
	 *             If a generated URL is illegal (shouldn't happen).
	 */
	public String addRunFeedEntry(String runID, Entry entry)
			throws FilesystemAccessException, NoDirectoryEntryException,
			UnknownRunException, NoUpdateException, MalformedURLException {
		TavernaRun run = support.getRun(runID);
		support.permitUpdate(run);
		// TODO Should this id be generated like this?
		String localId = "interact_" + currentTimeMillis();
		entry.setId("urn:uuid:" + randomUUID());
		String selfLink = uriBuilder.getRunUriBuilder(run)
				.path("feed/{entryID}").build(localId).toURL().toString();
		entry.addLink(selfLink);
		entry.setUpdated(new Date());
		putEntryInFile(utils.getDirectory(run, "feed"), localId + ".entry",
				entry);
		return selfLink;
	}
}
