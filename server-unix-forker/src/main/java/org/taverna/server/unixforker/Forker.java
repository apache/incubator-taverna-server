/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.unixforker;

import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A simple class that forks off processes when asked to over its standard
 * input. The one complication is that it forks them off as other users, through
 * the use of the <tt>sudo</tt> utility. It is Unix-specific.
 * 
 * @author Donal Fellows
 */
public class Forker extends Thread {
	private static String password;
	private static BufferedReader br;

	/**
	 * Helper to make reading a password from a file clearer. The password must
	 * be the first line of the file.
	 * 
	 * @param passwordFile
	 *            The file to load from.
	 * @throws IOException
	 *             If anything goes wrong.
	 */
	private static void loadPassword(@NonNull File passwordFile)
			throws IOException {
		FileReader fr = null;
		try {
			err.println("attempting to load password from " + passwordFile);
			fr = new FileReader(passwordFile);
			password = new BufferedReader(fr).readLine();
		} catch (IOException e) {
			err.println("failed to read password from file " + passwordFile
					+ "described in password.file property");
			throw e;
		} finally {
			if (fr != null)
				fr.close();
		}
	}

	/**
	 * Initialization code, which runs before the main loop starts processing.
	 * 
	 * @param args
	 *            The arguments to the program.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public static void init(String[] args) throws Exception {
		if (args.length < 1)
			throw new IllegalArgumentException(
					"wrong # args: must be \"program ?argument ...?\"");
		if (getProperty("password.file") != null)
			loadPassword(new File(getProperty("password.file")));
		if (password == null)
			err.println("no password.file property or empty file; "
					+ "assuming password-less sudo is configured");
		else
			err.println("password is of length " + password.length());
		br = new BufferedReader(new InputStreamReader(in));
	}

	/**
	 * The body of the main loop of this program.
	 * 
	 * @param args
	 *            The arguments to use when running the other program.
	 * @return Whether to repeat the loop.
	 * @throws Exception
	 *             If anything goes wrong. Note that the loop is repeated if an
	 *             exception occurs in it.
	 */
	public static boolean mainLoopBody(String[] args) throws Exception {
		String line = br.readLine();
		if (line == null)
			return false;
		List<String> vals = asList(line.split("[ \t]+"));
		if (vals.size() != 2) {
			out.println("wrong # values: must be \"username UUID\"");
			return true;
		}
		ProcessBuilder pb = new ProcessBuilder();
		pb.command()
				.addAll(asList("sudo", "-u", vals.get(0), "-S", "-H", "--"));
		pb.command().addAll(asList(args));
		pb.command().add(vals.get(1));
		Forker f = new Forker(pb);
		f.setDaemon(true);
		f.start();
		return true;
	}

	/**
	 * The main code for this class, which turns this into an executable
	 * program. Runs the initialisation and then the main loop, in both cases
	 * with appropriate error handling.
	 * 
	 * @param args
	 *            Arguments to this program.
	 */
	public static void main(String... args) {
		try {
			init(args);
			while (true) {
				try {
					if (!mainLoopBody(args))
						break;
				} catch (Exception e) {
					e.printStackTrace(err);
					out.println(e.getClass().getName() + ": " + e.getMessage());
				}
			}
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(err);
			System.exit(1);
		}
	}

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	public Forker(ProcessBuilder pb) throws IOException {
		out.println("Starting subprocess: " + pb.command());
		final Process p = pb.start();
		abstract class ProcessAttachedDaemon extends Thread {
			public ProcessAttachedDaemon() {
				setDaemon(true);
				start();
			}

			abstract void act() throws Exception;

			@Override
			public final void run() {
				try {
					act();
					p.waitFor();
				} catch (InterruptedException e) {
					// Just drop
				} catch (Exception e) {
					p.destroy();
					e.printStackTrace(err);
				}
			}
		}
		new ProcessAttachedDaemon() {
			@Override
			void act() throws Exception {
				copyFromSudo("Subprocess(out):", p.getInputStream());
			}
		};
		new ProcessAttachedDaemon() {
			@Override
			void act() throws Exception {
				copyFromSudo("Subprocess(err):", p.getErrorStream());
			}
		};
		new ProcessAttachedDaemon() {
			@Override
			void act() throws Exception {
				interactWithSudo(p.getOutputStream());
			}
		};
	}

	protected void interactWithSudo(OutputStream os) throws Exception {
		if (password != null) {
			OutputStreamWriter osw = new OutputStreamWriter(os);
			osw.write(password + "\n");
			osw.flush();
		}
		os.close();
	}

	protected void copyFromSudo(String header, InputStream sudoStream)
			throws Exception {
		int b = '\n';
		while (true) {
			if (b == '\n')
				out.print(header);
			b = sudoStream.read();
			if (b == -1)
				break;
			out.write(b);
			out.flush();
		}
		sudoStream.close();
	}
}
