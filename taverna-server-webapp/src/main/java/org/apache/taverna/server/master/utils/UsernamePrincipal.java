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

import java.io.Serializable;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A simple serializable principal that just records the name.
 * 
 * @author Donal Fellows
 */
public class UsernamePrincipal implements Principal, Serializable {
	private static final long serialVersionUID = 2703493248562435L;
	public UsernamePrincipal(String username) {
		this.name = username;
	}

	public UsernamePrincipal(Principal other) {
		this.name = other.getName();
	}

	public UsernamePrincipal(Authentication auth) {
		this(auth.getPrincipal());
	}

	public UsernamePrincipal(Object principal) {
		if (principal instanceof Principal)
			this.name = ((Principal) principal).getName();
		else if (principal instanceof String)
			this.name = (String) principal;
		else if (principal instanceof UserDetails)
			this.name = ((UserDetails) principal).getUsername();
		else
			this.name = principal.toString();
	}

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Principal<" + name + ">";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Principal) {
			Principal p = (Principal) o;
			return name.equals(p.getName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
