/*
 */
package org.taverna.server.master.utils;
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

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.taverna.server.master.exceptions.GeneralFailureException;

/**
 * Aspect used to convert {@linkplain RuntimeException runtime exceptions} into
 * a form that can be nicely conveyed to the outside world as HTTP errors.
 * 
 * @author Donal Fellows
 */
@Aspect
public class RuntimeExceptionWrapper {
	/**
	 * Map an unexpected exception to one that can be correctly reported as a
	 * problem.
	 * 
	 * @param exn
	 *            The runtime exception being trapped.
	 * @throws GeneralFailureException
	 *             The known exception type that it is mapped to.
	 */
	@AfterThrowing(pointcut = "execution(* org.taverna.server.master.rest..*(..)) && !bean(*Provider.*)", throwing = "exn")
	public void wrapRuntimeException(RuntimeException exn)
			throws GeneralFailureException {
		// Exclude security-related exceptions
		if (exn.getClass().getName().startsWith("org.springframework.security."))
			return;
		throw new GeneralFailureException(exn);
	}
}
