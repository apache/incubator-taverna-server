package org.taverna.server.master.exceptions;

import static org.taverna.server.master.common.Namespaces.SERVER_SOAP;

import javax.xml.ws.WebFault;

@WebFault(name = "GeneralFailureFault", targetNamespace = SERVER_SOAP)
public class GeneralFailureException extends RuntimeException {
	public GeneralFailureException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public GeneralFailureException(String message, Throwable cause) {
		super(message, cause);
	}
}
