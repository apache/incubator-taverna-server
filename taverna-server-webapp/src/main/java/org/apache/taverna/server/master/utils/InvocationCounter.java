/*
 */
package org.apache.taverna.server.master.utils;
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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * This class is responsible for counting all invocations of publicly-exposed
 * methods of the webapp. It's connected to the webapp primarily through an
 * AspectJ-style pointcut.
 * 
 * @author Donal Fellows
 */
@Aspect
public class InvocationCounter {
	private int count;

	@Before("@annotation(org.apache.taverna.server.master.utils.InvocationCounter.CallCounted)")
	public synchronized void count() {
		count++;
	}

	public synchronized int getCount() {
		return count;
	}

	/**
	 * Mark methods that should be counted by the invocation counter.
	 * 
	 * @author Donal Fellows
	 */
	@Retention(RUNTIME)
	@Documented
	@Target(METHOD)
	public static @interface CallCounted {
	}
}
