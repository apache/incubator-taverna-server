/*
 */
package org.apache.taverna.server.master.api;
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

/**
 * The content types supported at various points in the REST interface.
 * 
 * @author Donal Fellows
 */
public interface ContentTypes {
	/** "application/zip" */
	public static final MediaType APPLICATION_ZIP_TYPE = new MediaType(
			"application", "zip");

	/** "application/vnd.taverna.baclava+xml" */
	public static final MediaType BACLAVA_MEDIA_TYPE = new MediaType(
			"application", "vnd.taverna.baclava+xml");

	/**
	 * The media types that we are willing to serve up directories as. Note that
	 * we <i>only</i> serve directories up as these.
	 */
	public static final List<Variant> DIRECTORY_VARIANTS = asList(new Variant(
			APPLICATION_XML_TYPE, (String) null, "UTF-8"), new Variant(
			APPLICATION_JSON_TYPE, (String) null, "UTF-8"), new Variant(
			APPLICATION_ZIP_TYPE, (String) null, null));

	/**
	 * The baseline set of media types that we are willing to serve up files as.
	 * Note that we <i>also</i> serve files up as their auto-detected media
	 * type. In all cases, this means we just shovel the bytes (or characters,
	 * in the case of <tt>text/*</tt> subtypes) back at the client.
	 */
	public static final List<Variant> INITIAL_FILE_VARIANTS = singletonList(new Variant(
			APPLICATION_OCTET_STREAM_TYPE, (String) null, null));
}
