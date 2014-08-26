package org.taverna.server.master.localworker;

import static java.lang.Thread.interrupted;
import static org.apache.commons.logging.LogFactory.getLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;

abstract class StreamLogger {
	protected final Log log;
	private Thread t;
	private InputStream in;

	protected StreamLogger(final String name, InputStream is) {
		log = getLog("Taverna.Server.LocalWorker." + name);
		in = is;
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(in))) {
					String line;
					while (!interrupted() && (line = br.readLine()) != null)
						if (!line.isEmpty())
							write(line);
				} catch (IOException e) {
					// Do nothing...
				} catch (Exception e) {
					log.warn("failure in reading from " + name, e);
				}
			}
		}, name + ".StreamLogger");
		t.setContextClassLoader(null);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Write a line read from the subprocess to the log.
	 * <p>
	 * This needs to be implemented by subclasses in order for the log to be
	 * correctly written with the class name.
	 * 
	 * @param msg
	 *            The message to write. Guaranteed to have no newline characters
	 *            in it and to be non-empty.
	 */
	protected abstract void write(String msg);

	public void stop() {
		log.info("trying to close down " + t.getName());
		t.interrupt();
		try {
			in.close();
		} catch (IOException e) {
		}
	}
}
