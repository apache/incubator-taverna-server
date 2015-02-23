package org.taverna.server.localworker.impl.utils;

import javax.annotation.Nullable;

/**
 * A class that handles running a task that can take some time.
 * 
 * @author Donal Fellows
 * 
 */
public abstract class TimingOutTask extends Thread {
	public abstract void doIt() throws Exception;

	@Nullable
	private Exception ioe;

	@Override
	public final void run() {
		try {
			doIt();
		} catch (Exception ioe) {
			this.ioe = ioe;
		}
	}

	public TimingOutTask() {
		this.setDaemon(true);
	}

	public void doOrTimeOut(long timeout) throws Exception {
		start();
		try {
			join(timeout);
		} catch (InterruptedException e) {
			interrupt();
		}
		if (ioe != null)
			throw ioe;
	}
}