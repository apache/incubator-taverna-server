package org.taverna.server.localworker.impl;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.touch;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.ReferenceMap;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteDirectoryEntry;
import org.taverna.server.localworker.remote.RemoteFile;

/**
 * This class acts as a remote-aware delegate for the workflow run's working
 * directory and its subdirectories.
 * 
 * @author Donal Fellows
 * @see FileDelegate
 */
public class DirectoryDelegate extends UnicastRemoteObject implements
		RemoteDirectory {
	private File dir;
	private DirectoryDelegate parent;
	private ReferenceMap localCache;

	/**
	 * @param dir
	 * @param parent
	 * @throws RemoteException
	 *             If registration of the directory fails.
	 */
	public DirectoryDelegate(File dir, DirectoryDelegate parent)
			throws RemoteException {
		super();
		this.localCache = new ReferenceMap();
		this.dir = dir;
		this.parent = parent;
	}

	@Override
	public Collection<RemoteDirectoryEntry> getContents()
			throws RemoteException {
		ArrayList<RemoteDirectoryEntry> result = new ArrayList<RemoteDirectoryEntry>();
		for (String s : dir.list()) {
			if (s.equals(".") || s.equals(".."))
				continue;
			File f = new File(dir, s);
			RemoteDirectoryEntry entry = (RemoteDirectoryEntry) localCache
					.get(s);
			if (f.isDirectory()) {
				if (entry == null || !(entry instanceof DirectoryDelegate)) {
					entry = new DirectoryDelegate(f, this);
					localCache.put(s, entry);
				}
			} else if (f.isFile()) {
				if (entry == null || !(entry instanceof FileDelegate)) {
					entry = new FileDelegate(f, this);
					localCache.put(s, entry);
				}
			} else {
				// not file or dir; skip...
				continue;
			}
			result.add(entry);
		}
		return result;
	}

	@Override
	public RemoteFile makeEmptyFile(String name) throws IOException {
		if ("..".equals(name) || name.contains("/"))
			throw new IOException("illegal filename");
		File f = new File(dir, name);
		if (f.exists())
			throw new IOException("already exists");
		touch(f);
		FileDelegate delegate = new FileDelegate(f, this);
		localCache.put(name, delegate);
		return delegate;
	}

	@Override
	public RemoteDirectory makeSubdirectory(String name) throws IOException {
		if ("..".equals(name) || name.contains("/"))
			throw new IOException("illegal filename");
		File f = new File(dir, name);
		if (f.exists())
			throw new IOException("already exists");
		forceMkdir(f);
		DirectoryDelegate delegate = new DirectoryDelegate(f, this);
		localCache.put(name, delegate);
		return delegate;
	}

	@Override
	public void destroy() throws IOException {
		if (parent == null)
			throw new IOException("tried to destroy main job working directory");
		for (Object obj : localCache.values()) {
			if (obj == null)
				continue;
			try {
				((RemoteDirectoryEntry) obj).destroy();
			} catch (IOException e) {
			}
		}
		forceDelete(dir);
		parent.forgetEntry(this);
	}

	@Override
	public RemoteDirectory getContainingDirectory() {
		return parent;
	}

	void forgetEntry(RemoteDirectoryEntry entry) {
		MapIterator i = localCache.mapIterator();
		while (i.hasNext()) {
			Object key = i.next();
			if (entry == i.getValue()) {
				localCache.remove(key);
				break;
			}
		}
	}

	@Override
	public String getName() {
		if (parent == null)
			return "";
		return dir.getName();
	}
}
