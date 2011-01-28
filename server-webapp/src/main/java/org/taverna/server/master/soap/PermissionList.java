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
 * owner</i>.
 * 
 * @author Donal Fellows
 */
@XmlType
@XmlRootElement(name = "permissionList")
public class PermissionList {
	@XmlType(name = "")
	public static class SinglePermissionMapping {
		public SinglePermissionMapping(){}
		public SinglePermissionMapping(String user, Permission permission) {
			this.userName = user;
			this.permission = permission;
		}
		public String userName;
		public Permission permission;
	}

	@XmlElement
	public List<SinglePermissionMapping> permission;
}