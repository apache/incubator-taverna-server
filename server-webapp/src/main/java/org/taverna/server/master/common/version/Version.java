/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common.version;

import static org.taverna.server.master.common.version.Constants.PATCH;
import static org.taverna.server.master.common.version.Constants.VERSION;
import static org.taverna.server.master.common.version.Constants.alphaChar;
import static org.taverna.server.master.common.version.Constants.alphaHEnt;
import static org.taverna.server.master.common.version.Constants.alphaXEnt;

/**
 * Common location for describing the version of the server.
 * 
 * @author Donal Fellows
 */
public interface Version {
	public static final String JAVA = VERSION + alphaChar + PATCH;
	public static final String HTML = VERSION + alphaHEnt + PATCH;
	public static final String XML = VERSION + alphaXEnt + PATCH;
}

/**
 * The pieces of a version string.
 * 
 * @author Donal Fellows
 */
interface Constants {
	static final String MAJOR = "2";
	static final String MINOR = "5";
	static final String PATCH = "1";

	static final char alphaChar = '\u03b1';
	static final char betaChar = '\u03b2';
	static final char releaseChar = '.';
	static final String alphaHEnt = "&alpha;";
	static final String betaHEnt = "&beta;";
	static final String releaseHEnt = ".";
	static final String alphaXEnt = "&#x03b1;";
	static final String betaXEnt = "&#x03b2;";
	static final String releaseXEnt = ".";

	static final String VERSION = MAJOR + "." + MINOR;
}
