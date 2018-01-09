/*
 */
package org.apache.taverna.server.master.soap;
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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.taverna.server.master.common.Permission;

/**
 * The list of permissions to access a workflow run of users <i>other than the
 * owner</i>. This class exists to support the JAXB mapping.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "PermissionList")
@XmlRootElement(name = "permissionList")
public class PermissionList {
	/**
	 * The type of a single mapped permission. This class exists to support the
	 * JAXB mapping.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "")
	public static class SinglePermissionMapping {
		public SinglePermissionMapping() {
		}

		public SinglePermissionMapping(String user, Permission permission) {
			this.userName = user;
			this.permission = permission;
		}

		/** The name of the user that this talks about. */
		public String userName;
		/** The permission level that the user is granted. */
		public Permission permission;
	}

	/** The list of (non-default) permissions granted. */
	@XmlElement
	public List<SinglePermissionMapping> permission;
}