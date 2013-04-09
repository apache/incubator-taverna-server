/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

/**
 * A convenient place to keep the names of URIs so that they can be got right
 * <i>once</i>.
 * 
 * @author Donal Fellows
 */
public interface Namespaces {
	/**
	 * The XLink specification's namespace name.
	 */
	public static final String XLINK = "http://www.w3.org/1999/xlink";
	/**
	 * The XML Digital Signature specification's namespace name.
	 */
	public static final String XSIG = "http://www.w3.org/2000/09/xmldsig#";
	/**
	 * The Usage Record specification's namespace name.
	 */
	public static final String UR = "http://schema.ogf.org/urf/2003/09/urf";
	/**
	 * The namespace for the server.
	 */
	public static final String SERVER = "http://ns.taverna.org.uk/2010/xml/server/";
	public static final String SERVER_REST = SERVER + "rest/";
	public static final String SERVER_SOAP = SERVER + "soap/";
	public static final String FEED = SERVER + "feed/";
	public static final String ADMIN = SERVER + "admin/";
}
