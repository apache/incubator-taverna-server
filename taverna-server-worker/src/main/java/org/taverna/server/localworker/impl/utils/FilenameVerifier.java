/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.localworker.impl.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that handles filename validation on different target platforms.
 * 
 * @author Donal Fellows.
 */
public abstract class FilenameVerifier {
	private FilenameVerifier(){}

	static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

	@SuppressWarnings("serial")
	private static final Set<String> ILLEGAL_NAMES = new HashSet<String>(){{
		add("");
		add("..");
		add(".");
		if (IS_WINDOWS) {
			add("con");
			add("prn");
			add("nul");
			add("aux");
			for (int i = 1; i <= 9; i++) {
				add("com" + i);
				add("lpt" + i);
			}
		}
	}};
	@SuppressWarnings("serial")
	private static final Set<Character> ILLEGAL_CHARS = new HashSet<Character>(){{
		add('/');
		for (char i=0 ; i<32 ; i++)
			add(i);
		if (IS_WINDOWS) {
			add('\\');
			add('>');
			add('<');
			add(':');
			add('"');
			add('|');
			add('?');
			add('*');
		} else {
			add(' '); // whitespace; too much trouble from these
			add('\t');
			add('\r');
			add('\n');
		}
	}};
	@SuppressWarnings("serial")
	private static final Set<String> ILLEGAL_PREFIXES = new HashSet<String>(){{
		if (IS_WINDOWS) {
			add("con.");
			add("prn.");
			add("nul.");
			add("aux.");
			for (int i = 1; i <= 9; i++) {
				add("com" + i + ".");
				add("lpt" + i + ".");
			}
		}
	}};
	@SuppressWarnings("serial")
	private static final Set<String> ILLEGAL_SUFFIXES = new HashSet<String>(){{
		if (IS_WINDOWS) {
			add(" ");
			add(".");
		}
	}};

	/**
	 * Construct a file handle, applying platform-specific filename validation
	 * rules in the process.
	 * 
	 * @param dir
	 *            The directory acting as a root, which is assumed to be
	 *            correctly named. May be <tt>null</tt>.
	 * @param names
	 *            The names of filename fragments to apply the checks to. Must
	 *            have at least one value.
	 * @return The file handle. Never <tt>null</tt>.
	 * @throws IOException
	 *             If validation fails.
	 */
	public static File getValidatedFile(File dir, String... names)
			throws IOException {
		if (names.length == 0)
			throw new IOException("empty filename");
		File f = dir;
		for (String name : names) {
			String low = name.toLowerCase();
			if (ILLEGAL_NAMES.contains(low))
				throw new IOException("illegal filename");
			for (char c : ILLEGAL_CHARS)
				if (low.indexOf(c) >= 0)
					throw new IOException("illegal filename");
			for (String s : ILLEGAL_PREFIXES)
				if (low.startsWith(s))
					throw new IOException("illegal filename");
			for (String s : ILLEGAL_SUFFIXES)
				if (low.endsWith(s))
					throw new IOException("illegal filename");
			f = new File(f, name);
		}
		assert f != null;
		return f;
	}

	/**
	 * Create a file handle where the underlying file must exist.
	 * 
	 * @param dir
	 *            The directory that will contain the file.
	 * @param name
	 *            The name of the file; will be validated.
	 * @return The handle.
	 * @throws IOException
	 *             If validation fails or the file doesn't exist.
	 */
	public static File getValidatedExistingFile(File dir, String name)
			throws IOException {
		File f = getValidatedFile(dir, name);
		if (!f.exists())
			throw new IOException("doesn't exist");
		return f;
	}

	/**
	 * Create a file handle where the underlying file must <i>not</i> exist.
	 * 
	 * @param dir
	 *            The directory that will contain the file.
	 * @param name
	 *            The name of the file; will be validated.
	 * @return The handle. The file will not be created by this method.
	 * @throws IOException
	 *             If validation fails or the file does exist.
	 */
	public static File getValidatedNewFile(File dir, String name)
			throws IOException {
		File f = getValidatedFile(dir, name);
		if (f.exists())
			throw new IOException("already exists");
		return f;
	}
}
