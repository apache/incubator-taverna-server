/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.soap;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.taverna.server.master.common.Permission;

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