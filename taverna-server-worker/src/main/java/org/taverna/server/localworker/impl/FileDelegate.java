/*
 */
package org.taverna.server.localworker.impl;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import javax.annotation.Nonnull;

import org.taverna.server.localworker.remote.RemoteDirectory;
import org.taverna.server.localworker.remote.RemoteFile;

/**
 * This class acts as a remote-aware delegate for the files in a workflow run's
 * working directory and its subdirectories.
 * 
 * @author Donal Fellows
 * @see DirectoryDelegate
 */
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
	public FileDelegate(@Nonnull File file, @Nonnull DirectoryDelegate parent)
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
		int read;
		try (FileInputStream fis = new FileInputStream(file)) {
			if (offset > 0 && fis.skip(offset) != offset)
				throw new IOException("did not move to correct offset in file");
			read = fis.read(buffer);
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
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		}
	}

	@Override
	public void appendContents(byte[] data) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			fos.write(data);
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
