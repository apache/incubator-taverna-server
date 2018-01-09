/*
 */
package org.taverna.server.master.identity;
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
