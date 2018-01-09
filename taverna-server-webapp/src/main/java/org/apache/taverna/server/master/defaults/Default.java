/*
 */
package org.apache.taverna.server.master.defaults;
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

import org.apache.taverna.server.master.common.Status;
import org.apache.taverna.server.master.localworker.LocalWorkerState;

/**
 * This defines a collection of default values, collecting them from various
 * parts of the server.
 * 
 * @author Donal Fellows
 */
public interface Default {
	/** The default value of the <tt>prefix</tt> property. */
	static final String AUTHORITY_PREFIX = "LOCALUSER_";

	/**
	 * The name of the resource that is the implementation of the subprocess
	 * that this class will fork off.
	 */
	static final String SERVER_WORKER_IMPLEMENTATION_JAR = "util/server.worker.jar";

	/**
	 * The name of the resource that is the implementation of the subprocess
	 * that manages secure forking.
	 */
	static final String SECURE_FORK_IMPLEMENTATION_JAR = "util/secure.fork.jar";

	/**
	 * The name of the resource that is the implementation of the subprocess
	 * that acts as the RMI registry.
	 */
	static final String REGISTRY_JAR = "util/rmi.daemon.jar";

	/** Initial lifetime of runs, in minutes. */
	static final int RUN_LIFE_MINUTES = 20;

	/**
	 * Maximum number of runs to exist at once. Note that this includes when
	 * they are just existing for the purposes of file transfer (
	 * {@link Status#Initialized}/{@link Status#Finished} states).
	 */
	static final int RUN_COUNT_MAX = 5;

	/**
	 * Prefix to use for RMI names.
	 */
	static final String RMI_PREFIX = "ForkRunFactory.";

	/** Default value for {@link LocalWorkerState#passwordFile}. */
	static final String PASSWORD_FILE = null;

	/**
	 * The extra arguments to pass to the subprocess.
	 */
	static final String[] EXTRA_ARGUMENTS = new String[0];

	/**
	 * How long to wait for subprocess startup, in seconds.
	 */
	static final int SUBPROCESS_START_WAIT = 40;

	/**
	 * Polling interval to use during startup, in milliseconds.
	 */
	static final int SUBPROCESS_START_POLL_SLEEP = 1000;

	/**
	 * Maximum number of {@link Status#Operating} runs at any time.
	 */
	static final int RUN_OPERATING_LIMIT = 10;

	/**
	 * What fields of a certificate we look at when understanding who it is
	 * talking about, in the order that we look.
	 */
	static final String[] CERTIFICATE_FIELD_NAMES = { "CN", "COMMONNAME",
			"COMMON NAME", "COMMON_NAME", "OU", "ORGANIZATIONALUNITNAME",
			"ORGANIZATIONAL UNIT NAME", "O", "ORGANIZATIONNAME",
			"ORGANIZATION NAME" };

	/** The type of certificates that are processed if we don't say otherwise. */
	static final String CERTIFICATE_TYPE = "X.509";

	/** Max size of credential file, in kiB. */
	static final int CREDENTIAL_FILE_SIZE_LIMIT = 20;

	/**
	 * The notification message format to use if none is configured.
	 */
	public static final String NOTIFY_MESSAGE_FORMAT = "Your job with ID={0} has finished with exit code {1,number,integer}.";

	/** The address of the SMS gateway service used. */
	public static final String SMS_GATEWAY_URL = "https://www.intellisoftware.co.uk/smsgateway/sendmsg.aspx";
}
