/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

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
