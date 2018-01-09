/*
 */
package org.apache.taverna.server.master.common;
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

/**
 * The roles defined in this webapp.
 * 
 * @author Donal Fellows
 */
public interface Roles {
	/** The role of a normal user. */
	static final String USER = "ROLE_tavernauser";
	/**
	 * The role of an administrator. Administrators <i>should</i> have the
	 * normal user role as well.
	 */
	static final String ADMIN = "ROLE_tavernasuperuser";
	/**
	 * The role of a workflow accessing itself. Do not give users this role.
	 */
	static final String SELF = "ROLE_tavernaworkflow";
}
