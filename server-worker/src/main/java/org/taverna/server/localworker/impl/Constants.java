/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.localworker.impl;

import static java.nio.charset.Charset.defaultCharset;

/**
 * The defaults associated with this worker, together with various other
 * constants.
 * 
 * @author Donal Fellows
 */
public interface Constants {
	/**
	 * Subdirectories of the working directory to create by default.
	 */
	static final String[] SUBDIR_LIST = { "conf", "externaltool", "feed",
			"interactions", "lib", "logs", "plugins", "repository",
			"t2-database", "var" };

	/** The name of the default encoding for characters on this machine. */
	static final String SYSTEM_ENCODING = defaultCharset().name();

	/**
	 * Password to use to encrypt security information. This default is <7 chars
	 * to work even without Unlimited Strength JCE.
	 */
	static final char[] KEYSTORE_PASSWORD = { 'c', 'h', 'a', 'n', 'g', 'e' };

	/**
	 * The name of the directory (in the home directory) where security settings
	 * will be written.
	 */
	static final String SECURITY_DIR_NAME = ".taverna-server-security";

	/** The name of the file that will be the created keystore. */
	static final String KEYSTORE_FILE = "t2keystore.ubr";

	/** The name of the file that will be the created truststore. */
	static final String TRUSTSTORE_FILE = "t2truststore.ubr";

	/**
	 * The name of the file that contains the password to unlock the keystore
	 * and truststore.
	 */
	static final String PASSWORD_FILE = "password.txt";

	// --------- UNUSED ---------
	// /**
	// * The name of the file that contains the mapping from URIs to keystore
	// * aliases.
	// */
	// static final String URI_ALIAS_MAP = "urlmap.txt";

	/**
	 * Used to instruct the Taverna credential manager to use a non-default
	 * location for user credentials.
	 */
	static final String CREDENTIAL_MANAGER_DIRECTORY = "-cmdir";

	/**
	 * Used to instruct the Taverna credential manager to take its master
	 * password from standard input.
	 */
	static final String CREDENTIAL_MANAGER_PASSWORD = "-cmpassword";

	/**
	 * Name of environment variable used to pass HELIO security tokens to
	 * workflows.
	 */
	// This technique is known to be insecure; bite me.
	// TODO Use agreed environment name for HELIO CIS token
	static final String HELIO_TOKEN_NAME = "HELIO_CIS_TOKEN";

	/**
	 * The name of the standard listener, which is installed by default.
	 */
	static final String DEFAULT_LISTENER_NAME = "io";

	/**
	 * Time to wait for the subprocess to wait, in milliseconds.
	 */
	static final int START_WAIT_TIME = 1500;

	/**
	 * The name of the file (in this code's resources) that provides the default
	 * security policy that we use.
	 */
	static final String SECURITY_POLICY_FILE = "security.policy";

	/**
	 * The Java property holding security policy info.
	 */
	static final String SEC_POLICY_PROP = "java.security.policy";
	/**
	 * The Java property to set to make this code not try to enforce security
	 * policy.
	 */
	static final String UNSECURE_PROP = "taverna.suppressrestrictions.rmi";
	/**
	 * The Java property that holds the name of the host name to enforce.
	 */
	static final String RMI_HOST_PROP = "java.rmi.server.hostname";
	/**
	 * The default hostname to require in secure mode. This is the
	 * <i>resolved</i> version of "localhost".
	 */
	static final String LOCALHOST = "127.0.0.1";

	/**
	 * Time to wait during closing down this process. In milliseconds.
	 */
	static final int DEATH_DELAY = 500;
}
