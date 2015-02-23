/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static java.lang.Integer.MAX_VALUE;
import static javax.crypto.Cipher.getMaxAllowedKeyLength;
import static org.apache.commons.logging.LogFactory.getLog;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;

/**
 * Trivial bean that checks for whether the JCE policy files that allow
 * unlimited strength security are present, and warns in the log if not.
 * 
 * @author Donal Fellows
 */
public class JCECheck {
	/**
	 * Write a message to the log that says whether an unlimited strength
	 * {@linkplain #Cipher cipher} is present. This is the official proxy for
	 * whether the unlimited strength JCE policy files have been installed; if
	 * absent, the message is logged as a warning, otherwise it is just
	 * informational.
	 */
	@PostConstruct
	public void checkForUnlimitedJCE() {
		Log log = getLog("Taverna.Server.Utils");

		try {
			if (getMaxAllowedKeyLength("AES") < MAX_VALUE)
				log.warn("maximum key length very short; unlimited "
						+ "strength JCE policy files maybe missing");
			else
				log.info("unlimited strength JCE policy in place");
		} catch (GeneralSecurityException e) {
			log.warn("problem computing key length limits!", e);
		}
	}

	/**
	 * @return Whether the unlimited strength JCE policy files are present (or
	 *         rather whether an unlimited strength {@linkplain #Cipher cipher}
	 *         is permitted).
	 */
	public boolean isUnlimitedStrength() {
		try {
			return getMaxAllowedKeyLength("AES") == MAX_VALUE;
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}
}
