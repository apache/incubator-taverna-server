/*
 */
package org.taverna.server.master.exceptions;
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

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.WebFault;

/**
 * Exception thrown to indicate that no listener by that name exists, or that
 * some other problem with listeners has occurred.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoListenerFault")
@XmlSeeAlso(BadPropertyValueException.class)
public class NoListenerException extends Exception {
	private static final long serialVersionUID = -2550897312787546547L;

	public NoListenerException() {
		super("no such listener");
	}

	public NoListenerException(String msg) {
		super(msg);
	}

	public NoListenerException(String msg, Throwable t) {
		super(msg, t);
	}
}