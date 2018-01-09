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