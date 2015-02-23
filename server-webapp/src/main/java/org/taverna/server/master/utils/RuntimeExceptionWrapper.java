/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

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
