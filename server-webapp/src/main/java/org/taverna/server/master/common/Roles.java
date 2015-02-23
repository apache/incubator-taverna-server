/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

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
