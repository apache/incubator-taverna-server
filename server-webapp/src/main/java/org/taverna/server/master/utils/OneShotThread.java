package org.taverna.server.master.utils;

public class OneShotThread extends Thread {
	public OneShotThread(String name, Runnable target) {
		super(target, name);
		setContextClassLoader(null);
		setDaemon(true);
		start();
	}
}
