/*
 */
package org.apache.taverna.server.master.common.version;
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

import static org.apache.taverna.server.master.common.version.Constants.PATCH;
import static org.apache.taverna.server.master.common.version.Constants.VERSION;

/**
 * Common location for describing the version of the server.
 * 
 * @author Donal Fellows
 */
public interface Version {
	public static final String JAVA = VERSION + Constants.releaseChar + PATCH;
	public static final String HTML = VERSION + Constants.releaseHEnt + PATCH;
	public static final String XML = VERSION + Constants.releaseXEnt + PATCH;
}

/**
 * The pieces of a version string.
 * 
 * @author Donal Fellows
 */
interface Constants {
	static final String MAJOR = "3";
	static final String MINOR = "1";
	static final String PATCH = "0";

	static final char alphaChar = '\u03b1';
	static final char betaChar = '\u03b2';
	static final char releaseChar = '.';
	static final String alphaHEnt = "&alpha;";
	static final String betaHEnt = "&beta;";
	static final String releaseHEnt = ".";
	static final String alphaXEnt = "&#x03b1;";
	static final String betaXEnt = "&#x03b2;";
	static final String releaseXEnt = ".";

	static final String VERSION = MAJOR + "." + MINOR;
}
