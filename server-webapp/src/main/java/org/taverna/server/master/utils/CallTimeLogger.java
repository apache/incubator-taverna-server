/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.logging.LogFactory.getLog;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This class is responsible for timing all invocations of publicly-exposed
 * methods of the webapp. It's connected to the webapp through an AspectJ-style
 * pointcut that targets a custom annotation.
 * 
 * @author Donal Fellows
 */
@Aspect
public class CallTimeLogger {
	private long threshold = 100000;
	private Log log = getLog("Taverna.Server.Performance");

	public void setThreshold(long threshold) {
		this.threshold = threshold;
	}

	/**
	 * The timer for this aspect. The wrapped invocation will be timed, and a
	 * log message written if the configured threshold is exceeded.
	 * 
	 * @param call
	 *            The call being wrapped.
	 * @return The result of the call.
	 * @throws Throwable
	 *             If anything goes wrong with the wrapped call.
	 * @see System#nanoTime()
	 */
	@Around("@annotation(org.taverna.server.master.utils.CallTimeLogger.PerfLogged)")
	public Object time(ProceedingJoinPoint call) throws Throwable {
		long fore = nanoTime();
		try {
			return call.proceed();
		} finally {
			long aft = nanoTime();
			long elapsed = aft - fore;
			if (elapsed > threshold)
				log.info(format("call to %s took %dns", call.toShortString(),
						elapsed));
		}
	}

	/**
	 * Mark methods that should be counted by the invocation counter.
	 * 
	 * @author Donal Fellows
	 */
	@Retention(RUNTIME)
	@Documented
	@Target(METHOD)
	public static @interface PerfLogged {
	}
}
