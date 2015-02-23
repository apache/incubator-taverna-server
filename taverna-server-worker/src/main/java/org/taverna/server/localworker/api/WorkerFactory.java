package org.taverna.server.localworker.api;


/**
 * Class that manufactures instances of {@link Worker}.
 * 
 * @author Donal Fellows
 */
public interface WorkerFactory {
	/**
	 * Create an instance of the low-level worker class.
	 * 
	 * @return The worker object.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	Worker makeInstance() throws Exception;
}
