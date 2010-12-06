package org.taverna.server.master.localworker;

/**
 * How a user should be notified about the completion of a job.
 * 
 * @author Donal Fellows
 */
public interface CompletionNotifier {
	/**
	 * Called to notify someone or something that a workflow run has finished.
	 * 
	 * @param run
	 *            What run are we talking about.
	 * @param target
	 *            Who should be told.
	 * @param code
	 *            What the exit code was.
	 * @throws Exception
	 *             If anything fails; the exception will be logged.
	 */
	void notifyComplete(RemoteRunDelegate run, String target, int code)
			throws Exception;
}
