/*
 */
package org.apache.taverna.server.master.utils;
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
