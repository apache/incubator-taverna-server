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

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.WebFault;

/**
 * Exception that is thrown to indicate that the user is not permitted to update
 * something.
 * 
 * @author Donal Fellows
 */
@WebFault(name = "NoUpdateFault")
@XmlSeeAlso( { NoCreateException.class, NoDestroyException.class, BadStateChangeException.class })
public class NoUpdateException extends Exception {
	private static final long serialVersionUID = 4230987102653846379L;

	public NoUpdateException() {
		super("not permitted to update");
	}

	public NoUpdateException(String msg) {
		super(msg);
	}

	public NoUpdateException(String string, Throwable e) {
		super(string, e);
	}
}