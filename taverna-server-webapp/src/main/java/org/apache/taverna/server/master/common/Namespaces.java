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
	 * The T2flow document format's namespace name.
	 */
	public static final String T2FLOW = "http://taverna.sf.net/2008/xml/t2flow";
	/**
	 * The namespace for the server.
	 */
	public static final String SERVER = "http://ns.taverna.org.uk/2010/xml/server/";
	public static final String SERVER_REST = SERVER + "rest/";
	public static final String SERVER_SOAP = SERVER + "soap/";
	public static final String FEED = SERVER + "feed/";
	public static final String ADMIN = SERVER + "admin/";
}
