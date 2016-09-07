package org.taverna.server.master.api;
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

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.TavernaServerSupport;

/**
 * Indicates that this is a class that wants to be told by Spring about the
 * main support bean.
 * 
 * @author Donal Fellows
 */
public interface SupportAware {
	/**
	 * How to tell the bean about the support bean.
	 * 
	 * @param support
	 *            Reference to the support bean.
	 */
	@Required
	void setSupport(TavernaServerSupport support);
}