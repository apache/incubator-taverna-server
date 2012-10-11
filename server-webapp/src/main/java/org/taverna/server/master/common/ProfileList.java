/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.common;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Description of the profiles that can apply to a workflow.
 * 
 * @author Donal K. Fellows
 */
@XmlRootElement(name = "profiles")
@XmlType(name = "ProfileList")
public class ProfileList {
	public List<ProfileList.Info> profile = new ArrayList<ProfileList.Info>();

	/**
	 * Description of a single workflow profile.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "profile")
	@XmlType(name = "Profile")
	public static class Info {
		@XmlValue
		public String name;
		/**
		 * Whether this is the main profile.
		 */
		@XmlAttribute(name = "isMain")
		public Boolean main;
	}
}