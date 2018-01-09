/*
 */
package org.apache.taverna.server.master.interfaces;
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

import org.apache.taverna.server.master.utils.UsernamePrincipal;

/**
 * This interface describes how to map from the identity understood by the
 * webapp to the identity understood by the local execution system.
 * 
 * @author Donal Fellows
 */
public interface LocalIdentityMapper {
	/**
	 * Given a user's identity, get the local identity to use for executing
	 * their workflows. Note that it is assumed that there will never be a
	 * failure from this interface; it is <i>not</i> a security policy
	 * decision or enforcement point.
	 * 
	 * @param user
	 *            An identity token.
	 * @return A user name, which must be defined in the context that workflows
	 *         will be running in.
	 */
	public String getUsernameForPrincipal(UsernamePrincipal user);
}
