/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * Description of a permission to access a particular workflow run. Note that
 * users always have full access to their own runs, as does any user with the "
 * <tt>{@value org.taverna.server.master.common.Roles#ADMIN}</tt>" ability.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "Permission")
@XmlEnum
public enum Permission {
	/** Indicates that a user cannot see the workflow run at all. */
	@XmlEnumValue("none")
	None,
	/**
	 * Indicates that a user can see the workflow run and its contents, but
	 * can't modify anything.
	 */
	@XmlEnumValue("read")
	Read,
	/**
	 * Indicates that a user can update most aspects of a workflow, but cannot
	 * work with either its security features or its lifetime.
	 */
	@XmlEnumValue("update")
	Update,
	/**
	 * Indicates that a user can update almost all aspects of a workflow, with
	 * only its security features being shrouded.
	 */
	@XmlEnumValue("destroy")
	Destroy
}