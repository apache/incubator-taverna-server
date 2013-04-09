/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

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
			APPLICATION_XML_TYPE, null, null), new Variant(
			APPLICATION_JSON_TYPE, null, null), new Variant(
			APPLICATION_ZIP_TYPE, null, null));

	/**
	 * The baseline set of media types that we are willing to serve up files as.
	 * Note that we <i>also</i> serve files up as their auto-detected media
	 * type. In all cases, this means we just shovel the bytes (or characters,
	 * in the case of <tt>text/*</tt> subtypes) back at the client.
	 */
	public static final List<Variant> INITIAL_FILE_VARIANTS = singletonList(new Variant(
			APPLICATION_OCTET_STREAM_TYPE, null, null));
}
