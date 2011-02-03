/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.Calendar.MINUTE;
import static java.util.Collections.unmodifiableSet;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.localworker.AbstractRemoteRunFactory.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.taverna.server.localworker.remote.IllegalStateTransitionException;
import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteDirectoryEntry;
import org.taverna.server.localworker.remote.RemoteFile;
import org.taverna.server.localworker.remote.RemoteInput;
import org.taverna.server.localworker.remote.RemoteListener;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.localworker.remote.RemoteStatus;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;

/**
 * Bridging shim between the WebApp world and the RMI world.
 * 
 * @author Donal Fellows
 */
public class RemoteRunDelegate implements TavernaRun {
	String id;
	private Date creationInstant;
	private Workflow workflow;
	private Date expiry;
	transient RemoteSingleRun run;
	private transient TavernaSecurityContext secContext;
	boolean doneTransitionToFinished;
	HashSet<String> readers;
	HashSet<String> writers;
	HashSet<String> destroyers;

	RemoteRunDelegate(Date creationInstant, Workflow workflow,
			RemoteSingleRun rsr, int defaultLifetime) {
		if (rsr == null) {
			throw new IllegalArgumentException("remote run must not be null");
		}
		this.creationInstant = creationInstant;
		this.workflow = workflow;
		Calendar c = Calendar.getInstance();
		c.add(MINUTE, defaultLifetime);
		this.expiry = c.getTime();
		this.run = rsr;
	}

	@Override
	public void addListener(Listener listener) {
		if (listener instanceof ListenerDelegate)
			try {
				run.addListener(((ListenerDelegate) listener).getRemote());
			} catch (RemoteException e) {
				log.warn("problem adding listener", e);
			}
		else
			log.fatal("bad listener " + listener.getClass()
					+ "; not applicable remotely!");
	}

	@Override
	public String getID() {
		if (id == null)
			id = randomUUID().toString();
		return id;
	}

	public Listener makeListener(String type, String config)
			throws NoListenerException {
		try {
			return new ListenerDelegate(run.makeListener(type, config));
		} catch (RemoteException e) {
			throw new NoListenerException("failed to make listener", e);
		}
	}

	@Override
	public void destroy() {
		try {
			run.destroy();
		} catch (RemoteException e) {
			log.warn("failed to destroy run", e);
		}
	}

	private static class ListenerDelegate implements Listener {
		private RemoteListener r;
		String conf;

		ListenerDelegate(RemoteListener l) {
			r = l;
		}

		RemoteListener getRemote() {
			return r;
		}

		@Override
		public String getConfiguration() {
			try {
				if (conf == null)
					conf = r.getConfiguration();
			} catch (RemoteException e) {
				log.warn("failed to get configuration", e);
			}
			return conf;
		}

		@Override
		public String getName() {
			try {
				return r.getName();
			} catch (RemoteException e) {
				log.warn("failed to get name", e);
				return "UNKNOWN NAME";
			}
		}

		@Override
		public String getProperty(String propName) throws NoListenerException {
			try {
				return r.getProperty(propName);
			} catch (RemoteException e) {
				throw new NoListenerException("no such property: " + propName,
						e);
			}
		}

		@Override
		public String getType() {
			try {
				return r.getType();
			} catch (RemoteException e) {
				log.warn("failed to get type", e);
				return "UNKNOWN TYPE";
			}
		}

		@Override
		public String[] listProperties() {
			try {
				return r.listProperties();
			} catch (RemoteException e) {
				log.warn("failed to list properties", e);
				return new String[0];
			}
		}

		@Override
		public void setProperty(String propName, String value)
				throws NoListenerException, BadPropertyValueException {
			try {
				r.setProperty(propName, value);
			} catch (RemoteException e) {
				log.warn("failed to set property", e);
				if (e.getCause() != null
						&& e.getCause() instanceof RuntimeException)
					throw new NoListenerException("failed to set property",
							e.getCause());
				if (e.getCause() != null && e.getCause() instanceof Exception)
					throw new BadPropertyValueException(
							"failed to set property", e.getCause());
				throw new BadPropertyValueException("failed to set property", e);
			}
		}
	}

	@Override
	public Date getExpiry() {
		return expiry;
	}

	@Override
	public List<Listener> getListeners() {
		ArrayList<Listener> listeners = new ArrayList<Listener>();
		try {
			for (RemoteListener rl : run.getListeners()) {
				listeners.add(new ListenerDelegate(rl));
			}
		} catch (RemoteException e) {
			log.warn("failed to get listeners", e);
		}
		return listeners;
	}

	@Override
	public TavernaSecurityContext getSecurityContext() {
		return secContext;
	}

	@Override
	public Status getStatus() {
		try {
			switch (run.getStatus()) {
			case Initialized:
				return Status.Initialized;
			case Operating:
				return Status.Operating;
			case Stopped:
				return Status.Stopped;
			case Finished:
				return Status.Finished;
			}
		} catch (RemoteException e) {
			log.warn("problem getting remote status", e);
		}
		return Status.Finished;
	}

	@Override
	public Workflow getWorkflow() {
		return workflow;
	}

	@Override
	public Directory getWorkingDirectory() throws FilesystemAccessException {
		try {
			return new DirectoryDelegate(run.getWorkingDirectory());
		} catch (Throwable e) {
			if (e.getCause() != null)
				e = e.getCause();
			throw new FilesystemAccessException(
					"problem getting main working directory handle", e);
		}
	}

	private abstract static class DEDelegate implements DirectoryEntry {
		private RemoteDirectoryEntry entry;
		private String name;

		DEDelegate(RemoteDirectoryEntry entry) {
			this.entry = entry;
		}

		@Override
		public void destroy() throws FilesystemAccessException {
			try {
				entry.destroy();
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to delete directory entry", e);
			}
		}

		@Override
		public String getFullName() {
			String n = getName();
			RemoteDirectoryEntry re = entry;
			try {
				while (true) {
					RemoteDirectory parent = re.getContainingDirectory();
					if (parent == null)
						break;
					n = parent.getName() + "/" + n;
					re = parent;
				}
			} catch (RemoteException e) {
				log.warn("failed to generate full name", e);
			}
			return n;
		}

		@Override
		public String getName() {
			if (name == null)
				try {
					name = entry.getName();
				} catch (RemoteException e) {
					log.error("failed to get name", e);
				}
			return name;
		}
	}

	private static class DirectoryDelegate extends DEDelegate implements
			Directory {
		private RemoteDirectory rd;

		DirectoryDelegate(RemoteDirectory dir) {
			super(dir);
			rd = dir;
		}

		@Override
		public Collection<DirectoryEntry> getContents()
				throws FilesystemAccessException {
			ArrayList<DirectoryEntry> result = new ArrayList<DirectoryEntry>();
			try {
				for (RemoteDirectoryEntry rde : rd.getContents()) {
					if (rde instanceof RemoteDirectory)
						result.add(new DirectoryDelegate((RemoteDirectory) rde));
					else
						result.add(new FileDelegate((RemoteFile) rde));
				}
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to get directory contents", e);
			}
			return result;
		}

		@Override
		public File makeEmptyFile(Principal actor, String name)
				throws FilesystemAccessException {
			try {
				return new FileDelegate(rd.makeEmptyFile(name));
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to make empty file", e);
			}
		}

		@Override
		public Directory makeSubdirectory(Principal actor, String name)
				throws FilesystemAccessException {
			try {
				return new DirectoryDelegate(rd.makeSubdirectory(name));
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to make subdirectory", e);
			}
		}

		@Override
		public byte[] getContentsAsZip() throws FilesystemAccessException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			try {
				zipDirectory(this.rd, null, zos);
				zos.close();
				zos = null;
				return baos.toByteArray();
			} catch (Exception e) {
				throw new FilesystemAccessException(
						"failed to zip up directory", e);
			} finally {
				if (zos != null)
					try {
						zos.close();
					} catch (IOException e) {
						// Ignore this; it should be impossible.
					}
			}
		}

		/**
		 * Compresses a directory tree into a ZIP.
		 * 
		 * @param dir
		 *            The directory to compress.
		 * @param base
		 *            The base name of the directory (or <tt>null</tt> if this
		 *            is the root directory of the ZIP).
		 * @param zos
		 *            Where to write the compressed data.
		 * @throws RemoteException
		 *             If some kind of problem happens with the remote
		 *             delegates.
		 * @throws IOException
		 *             If we run into problems with reading or writing data.
		 */
		private void zipDirectory(RemoteDirectory dir, String base,
				ZipOutputStream zos) throws RemoteException, IOException {
			for (RemoteDirectoryEntry rde : dir.getContents()) {
				String name = rde.getName();
				if (base != null)
					name = base + "/" + name;
				if (rde instanceof RemoteDirectory) {
					RemoteDirectory rd = (RemoteDirectory) rde;
					zipDirectory(rd, name, zos);
				} else {
					RemoteFile rf = (RemoteFile) rde;
					zos.putNextEntry(new ZipEntry(name));
					try {
						int off = 0;
						while (true) {
							byte[] c = rf.getContents(off, 64 * 1024);
							if (c == null || c.length == 0)
								break;
							zos.write(c);
							off += c.length;
						}
					} finally {
						zos.closeEntry();
					}
				}
			}
		}
	}

	private static class FileDelegate extends DEDelegate implements File {
		RemoteFile rf;

		FileDelegate(RemoteFile f) {
			super(f);
			this.rf = f;
		}

		@Override
		public byte[] getContents(int offset, int length)
				throws FilesystemAccessException {
			try {
				return rf.getContents(offset, length);
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to read file contents", e);
			}
		}

		@Override
		public long getSize() throws FilesystemAccessException {
			try {
				return rf.getSize();
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to get file length", e);
			}
		}

		@Override
		public void setContents(byte[] data) throws FilesystemAccessException {
			try {
				rf.setContents(data);
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to write file contents", e);
			}
		}

		@Override
		public void appendContents(byte[] data)
				throws FilesystemAccessException {
			try {
				rf.appendContents(data);
			} catch (IOException e) {
				throw new FilesystemAccessException(
						"failed to write file contents", e);
			}
		}
	}

	@Override
	public void setExpiry(Date d) {
		if (d.after(new Date()))
			expiry = d;
	}

	@Override
	public void setStatus(Status s) throws BadStateChangeException {
		try {
			switch (s) {
			case Initialized:
				run.setStatus(RemoteStatus.Initialized);
				break;
			case Operating:
				if (run.getStatus() == RemoteStatus.Initialized) {
					secContext.conveySecurity();
				}
				run.setStatus(RemoteStatus.Operating);
				break;
			case Stopped:
				run.setStatus(RemoteStatus.Stopped);
				break;
			case Finished:
				run.setStatus(RemoteStatus.Finished);
				break;
			}
		} catch (IllegalStateTransitionException e) {
			throw new BadStateChangeException(e.getMessage());
		} catch (RemoteException e) {
			throw new BadStateChangeException(e.getMessage(), e.getCause());
		} catch (GeneralSecurityException e) {
			throw new BadStateChangeException(e.getMessage(), e);
		} catch (IOException e) {
			throw new BadStateChangeException(e.getMessage(), e);
		}
	}

	static void checkBadFilename(String filename)
			throws FilesystemAccessException {
		if (filename.startsWith("/"))
			throw new FilesystemAccessException("filename may not be absolute");
		if (Arrays.asList(filename.split("/")).contains(".."))
			throw new FilesystemAccessException(
					"filename may not refer to parent");
	}

	private static class RunInput implements Input {
		private final RemoteInput i;

		RunInput(RemoteInput remote) {
			this.i = remote;
		}

		@Override
		public String getFile() {
			try {
				return i.getFile();
			} catch (RemoteException e) {
				return null;
			}
		}

		@Override
		public String getName() {
			try {
				return i.getName();
			} catch (RemoteException e) {
				return null;
			}
		}

		@Override
		public String getValue() {
			try {
				return i.getValue();
			} catch (RemoteException e) {
				return null;
			}
		}

		@Override
		public void setFile(String file) throws FilesystemAccessException,
				BadStateChangeException {
			checkBadFilename(file);
			try {
				i.setFile(file);
			} catch (RemoteException e) {
				throw new FilesystemAccessException(
						"cannot set file for input", e);
			}
		}

		@Override
		public void setValue(String value) throws BadStateChangeException {
			try {
				i.setValue(value);
			} catch (RemoteException e) {
				throw new BadStateChangeException(e);
			}
		}
	}

	@Override
	public String getInputBaclavaFile() {
		try {
			return run.getInputBaclavaFile();
		} catch (RemoteException e) {
			log.warn("problem when fetching input baclava file", e);
			return null;
		}
	}

	@Override
	public List<Input> getInputs() {
		ArrayList<Input> inputs = new ArrayList<Input>();
		try {
			for (RemoteInput ri : run.getInputs()) {
				inputs.add(new RunInput(ri));
			}
		} catch (RemoteException e) {
			log.warn("problem when fetching list of workflow inputs", e);
		}
		return inputs;
	}

	@Override
	public String getOutputBaclavaFile() {
		try {
			return run.getOutputBaclavaFile();
		} catch (RemoteException e) {
			log.warn("problem when fetching output baclava file", e);
			return null;
		}
	}

	@Override
	public Input makeInput(String name) throws BadStateChangeException {
		try {
			return new RunInput(run.makeInput(name));
		} catch (RemoteException e) {
			throw new BadStateChangeException("failed to make input", e);
		}
	}

	@Override
	public void setInputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException {
		checkBadFilename(filename);
		try {
			run.setInputBaclavaFile(filename);
		} catch (RemoteException e) {
			throw new FilesystemAccessException(
					"cannot set input baclava file name", e);
		}
	}

	@Override
	public void setOutputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException {
		checkBadFilename(filename);
		try {
			run.setOutputBaclavaFile(filename);
		} catch (RemoteException e) {
			throw new FilesystemAccessException(
					"cannot set output baclava file name", e);
		}
	}

	@Override
	public Date getCreationTimestamp() {
		return creationInstant;
	}

	@Override
	public Date getFinishTimestamp() {
		try {
			return run.getFinishTimestamp();
		} catch (RemoteException e) {
			log.info("failed to get finish timestamp", e);
			return null;
		}
	}

	@Override
	public Date getStartTimestamp() {
		try {
			return run.getStartTimestamp();
		} catch (RemoteException e) {
			log.info("failed to get finish timestamp", e);
			return null;
		}
	}

	/**
	 * @param readers
	 *            the readers to set
	 * @param db
	 *            the database of runs
	 */
	public void setReaders(Set<String> readers, RunDatabase db) {
		this.readers = new HashSet<String>(readers);
		db.flushToDisk(this);
	}

	/**
	 * @return the readers
	 */
	public Set<String> getReaders() {
		return readers == null ? new HashSet<String>()
				: unmodifiableSet(readers);
	}

	/**
	 * @param writers
	 *            the writers to set
	 * @param db
	 *            the database of runs
	 */
	public void setWriters(Set<String> writers, RunDatabase db) {
		this.writers = new HashSet<String>(writers);
		db.flushToDisk(this);
	}

	/**
	 * @return the writers
	 */
	public Set<String> getWriters() {
		return writers == null ? new HashSet<String>()
				: unmodifiableSet(writers);
	}

	/**
	 * @param destroyers
	 *            the destroyers to set
	 * @param db
	 *            the database of runs
	 */
	public void setDestroyers(Set<String> destroyers, RunDatabase db) {
		this.destroyers = new HashSet<String>(destroyers);
		db.flushToDisk(this);
	}

	/**
	 * @return the destroyers
	 */
	public Set<String> getDestroyers() {
		return destroyers == null ? new HashSet<String>()
				: unmodifiableSet(destroyers);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeUTF(secContext.getOwner().getName());
		out.writeObject(secContext.getFactory());
		out.writeObject(new MarshalledObject<RemoteSingleRun>(run));
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		final String creatorName = in.readUTF();
		SecurityContextFactory factory = (SecurityContextFactory) in
				.readObject();
		try {
			secContext = factory.create(this, new Principal() {
				@Override
				public String getName() {
					return creatorName;
				}

				@Override
				public boolean equals(Object o) {
					if (o instanceof Principal) {
						Principal p = (Principal) o;
						return creatorName.equals(p.getName());
					}
					return false;
				}

				@Override
				public int hashCode() {
					return creatorName.hashCode();
				}
			});
		} catch (RuntimeException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new SecurityContextReconstructionException(e);
		}
		run = ((MarshalledObject<RemoteSingleRun>) in.readObject()).get();
	}

	void setSecurityContext(TavernaSecurityContext tavernaSecurityContext) {
		secContext = tavernaSecurityContext;
	}
}

class SecurityContextReconstructionException extends RuntimeException {
	public SecurityContextReconstructionException(Throwable t) {
		super("failed to rebuild security context", t);
	}
}