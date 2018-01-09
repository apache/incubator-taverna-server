/*
 */
package org.apache.taverna.server.master.exceptions;
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

import javax.xml.ws.WebFault;

/**
 * Exception that is thrown to indicate that the state change requested for a
 * run is currently impossible due to excessive server load.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "OverloadedFault")
public class OverloadedException extends BadStateChangeException {
	private static final long serialVersionUID = 490826388447601776L;

	public OverloadedException() {
		super("server too busy; try later please");
	}

	public OverloadedException(Throwable t) {
		super("server too busy; try later please", t);
	}

	public OverloadedException(String msg, Throwable t) {
		super(msg, t);
	}

	public OverloadedException(String message) {
		super(message);
	}
}
