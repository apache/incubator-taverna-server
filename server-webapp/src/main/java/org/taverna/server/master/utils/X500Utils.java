/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static javax.security.auth.x500.X500Principal.RFC2253;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.annotation.PreDestroy;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support class that factors out some of the messier parts of working with
 * X.500 identities and X.509 certificates.
 * 
 * @author Donal Fellows
 */
public class X500Utils {
	private Log log = LogFactory.getLog("Taverna.Server.Utils");

	@PreDestroy
	void closeLog() {
		log = null;
	}

	private static final char DN_SEPARATOR = ',';
	private static final char DN_ESCAPE = '\\';
	private static final char DN_QUOTE = '"';

	/**
	 * Parse the DN from the Principal and extract the CN field.
	 * 
	 * @param id
	 *            The identity to extract the distinguished name from.
	 * @param fields
	 *            The names to look at when finding the field to return. Each
	 *            should be an upper-cased string.
	 * @return The common-name part of the distinguished name, or the literal
	 *         string "<tt>none</tt>" if there is no CN.
	 */
	public String getName(X500Principal id, String... fields) {
		String dn = id.getName(RFC2253);

		int i = 0;
		int startIndex = 0;
		boolean ignoreThisChar = false;
		boolean inQuotes = false;
		HashMap<String, String> tokenized = new HashMap<String, String>();

		for (i = 0; i < dn.length(); i++)
			if (ignoreThisChar)
				ignoreThisChar = false;
			else if (dn.charAt(i) == DN_QUOTE)
				inQuotes = !inQuotes;
			else if (inQuotes)
				continue;
			else if (dn.charAt(i) == DN_ESCAPE)
				ignoreThisChar = true;
			else if ((dn.charAt(i) == DN_SEPARATOR) && !ignoreThisChar) {
				storeDNField(tokenized, dn.substring(startIndex, i).trim()
						.split("=", 2));
				startIndex = i + 1;
			}
		if (inQuotes || ignoreThisChar)
			log.warn("was parsing invalid DN format");
		// Add last token - after the last delimiter
		storeDNField(tokenized, dn.substring(startIndex).trim().split("=", 2));

		for (String field : fields) {
			String value = tokenized.get(field);
			if (value != null)
				return value;
		}
		return "none";
	}

	private void storeDNField(HashMap<String, String> container, String[] split) {
		if (split == null || split.length != 2)
			return;
		String key = split[0].toUpperCase();
		if (container.containsKey(key))
			log.warn("duplicate field in DN: " + key);
		// LATER: Should the field be de-quoted?
		container.put(key, split[1]);
	}

	/**
	 * Get the serial number from a certificate as a hex string.
	 * 
	 * @param cert
	 *            The certificate to extract from.
	 * @return A hex string, in upper-case.
	 */
	public String getSerial(X509Certificate cert) {
		return new BigInteger(1, cert.getSerialNumber().toByteArray())
				.toString(16).toUpperCase();
	}
}