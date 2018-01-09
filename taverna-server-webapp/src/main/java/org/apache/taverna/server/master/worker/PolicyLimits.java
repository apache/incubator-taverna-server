/*
 */
package org.apache.taverna.server.master.worker;
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

import java.net.URI;
import java.util.List;

import org.apache.taverna.server.master.common.Status;

/**
 * The worker policy delegates certain limits to the state model of the
 * particular worker.
 * 
 * @author Donal Fellows
 */
public interface PolicyLimits {
	/**
	 * @return the maximum number of extant workflow runs in any state
	 */
	int getMaxRuns();

	/**
	 * @return the maximum number of workflow runs in the
	 *         {@linkplain Status#Operating operating} state.
	 */
	int getOperatingLimit();

	/**
	 * @return the list of URIs to workflows that may be used to create workflow
	 *         runs. If empty or <tt>null</tt>, no restriction is present.
	 */
	List<URI> getPermittedWorkflowURIs();

	/**
	 * @param permitted
	 *            the list of URIs to workflows that may be used to create
	 *            workflow runs.
	 */
	void setPermittedWorkflowURIs(List<URI> permitted);
}
