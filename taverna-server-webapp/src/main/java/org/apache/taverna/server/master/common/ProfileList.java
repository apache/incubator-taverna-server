/*
 */
package org.taverna.server.master.common;
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