/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

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

	@Before("@annotation(org.taverna.server.master.utils.InvocationCounter.CallCounted)")
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
