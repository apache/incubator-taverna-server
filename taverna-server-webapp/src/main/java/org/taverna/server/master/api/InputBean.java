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

import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.ContentsDescriptorBuilder;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.rest.TavernaServerInputREST;
import org.taverna.server.master.utils.FilenameUtils;

/**
 * Description of properties supported by {@link InputREST}.
 * 
 * @author Donal Fellows
 */
public interface InputBean extends SupportAware {
	TavernaServerInputREST connect(TavernaRun run, UriInfo ui);

	void setCdBuilder(ContentsDescriptorBuilder cd);

	void setFileUtils(FilenameUtils fn);
}