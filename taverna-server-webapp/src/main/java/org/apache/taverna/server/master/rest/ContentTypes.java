/*
 */
package org.apache.taverna.server.master.rest;
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

import static javax.ws.rs.core.MediaType.APPLICATION_ATOM_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Miscellaneous content type constants.
 * 
 * @author Donal Fellows
 */
interface ContentTypes {
	static final String URI_LIST = "text/uri-list";
	static final String ZIP = "application/zip";
	static final String TEXT = TEXT_PLAIN;
	static final String XML = APPLICATION_XML;
	static final String JSON = APPLICATION_JSON;
	static final String BYTES = APPLICATION_OCTET_STREAM;
	static final String ATOM = APPLICATION_ATOM_XML;
	static final String ROBUNDLE = "application/vnd.wf4ever.robundle+zip";
}
