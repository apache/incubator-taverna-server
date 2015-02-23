/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.identity;

import static java.util.regex.Pattern.compile;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.taverna.server.master.interfaces.LocalIdentityMapper;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * A trivial identity mapper that just uses the name out of the
 * {@link Principal}, or uses a regular expression to extract it from the string
 * representation of the principal.
 * 
 * @author Donal Fellows
 */
public class NameIDMapper implements LocalIdentityMapper {
	private Pattern pat;

	/**
	 * @param regexp
	 *            The regular expression to use. The first capturing group
	 *            within the RE will be the result of the extraction.
	 * @throws PatternSyntaxException
	 *             If the pattern is invalid.
	 */
	public void setRegexp(String regexp) throws PatternSyntaxException {
		pat = compile(regexp);
	}

	@Override
	public String getUsernameForPrincipal(UsernamePrincipal user) {
		if (pat != null) {
			Matcher m = pat.matcher(user.toString());
			if (m.find() && m.groupCount() > 0) {
				return m.group(1);
			}
			return null;
		}
		return user.getName();
	}
}
