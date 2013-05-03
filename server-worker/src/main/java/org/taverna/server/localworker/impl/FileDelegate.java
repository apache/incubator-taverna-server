/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.lang.System.arraycopy;
import static java.net.InetAddress.getLocalHost;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.forceDelete;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;

import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteFile;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * This class acts as a remote-aware delegate for the files in a workflow run's
 * working directory and its subdirectories.
 * 
 * @author Donal Fellows
 * @see DirectoryDelegate
 */
@SuppressWarnings("SE_NO_SERIALVERSIONID")
@java.lang.SuppressWarnings("serial")
public class FileDelegate extends UnicastRemoteObject implements RemoteFile {
	private File file;
	private DirectoryDelegate parent;

	/**
	 * @param file
	 * @param parent
	 * @throws RemoteException
	 *             If registration of the file fails.
	 */
	public FileDelegate(@NonNull File file, @NonNull DirectoryDelegate parent)
			throws RemoteException {
		super();
		this.file = file;
		this.parent = parent;
	}

	@Override
	public byte[] getContents(int offset, int length) throws IOException {
		if (length == -1)
			length = (int) (file.length() - offset);
		if (length < 0 || length > 1024 * 64)
			length = 1024 * 64;
		byte[] buffer = new byte[length];
		FileInputStream fis = null;
		int read;
		try {
			fis = new FileInputStream(file);
			if (offset > 0)
				if (fis.skip(offset) != offset)
					throw new IOException("did not move to correct offset in file");
			read = fis.read(buffer);
		} finally {
			if (fis != null)
				fis.close();
		}
		if (read <= 0)
			return new byte[0];
		if (read < buffer.length) {
			byte[] shortened = new byte[read];
			arraycopy(buffer, 0, shortened, 0, read);
			return shortened;
		}
		return buffer;
	}

	@Override
	public long getSize() {
		return file.length();
	}

	@Override
	public void setContents(byte[] data) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fos.write(data);
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	@Override
	public void appendContents(byte[] data) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, true);
			fos.write(data);
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	@Override
	public void destroy() throws IOException {
		forceDelete(file);
		parent.forgetEntry(this);
		parent = null;
	}

	@Override
	public RemoteDirectory getContainingDirectory() {
		return parent;
	}

	@Override
	public String getName() {
		return file.getName();
	}

	@Override
	public void copy(RemoteFile sourceFile) throws RemoteException, IOException {
		String sourceHost = sourceFile.getNativeHost();
		if (!getNativeHost().equals(sourceHost)) {
			throw new IOException(
					"cross-system copy not implemented; cannot copy from "
							+ sourceHost + " to " + getNativeHost());
		}
		// Must copy; cannot count on other file to stay unmodified
		copyFile(new File(sourceFile.getNativeName()), file);
	}

	@Override
	public String getNativeName() {
		return file.getAbsolutePath();
	}

	@Override
	public String getNativeHost() {
		try {
			return getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			throw new RuntimeException(
					"unexpected failure to resolve local host address", e);
		}
	}

	@Override
	public Date getModificationDate() throws RemoteException {
		return new Date(file.lastModified());
	}
}
