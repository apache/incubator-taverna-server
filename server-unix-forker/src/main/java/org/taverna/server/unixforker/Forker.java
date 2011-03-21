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
		FileReader fr = null;
		try {
			fr = new FileReader(new File(getProperty("password.file")));
			password = new BufferedReader(fr).readLine();
		} catch (IOException e) {
			err.println("failed to read password from file "
					+ "described in password.file property");
			throw e;
		} finally {
			if (fr != null)
				fr.close();
		}
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
		Thread tFromSudoStdout = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					copyFromSudo("Subprocess(out):", p.getInputStream());
					p.waitFor();
				} catch (Exception e) {
					p.destroy();
					e.printStackTrace(err);
				}
			}
		});
		tFromSudoStdout.setDaemon(true);
		tFromSudoStdout.start();
		Thread tFromSudoStderr = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					copyFromSudo("Subprocess(err):", p.getErrorStream());
					p.waitFor();
				} catch (Exception e) {
					p.destroy();
					e.printStackTrace(err);
				}
			}
		});
		tFromSudoStderr.setDaemon(true);
		tFromSudoStderr.start();
		Thread tToSudoStdin = new Thread(new Runnable() {
			@Override
			public final void run() {
				try {
					interactWithSudo(p.getOutputStream());
					p.waitFor();
				} catch (Exception e) {
					p.destroy();
					e.printStackTrace(err);
				}
			}
		});
		tToSudoStdin.setDaemon(true);
		tToSudoStdin.start();
	}

	protected void interactWithSudo(OutputStream os) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(os);
		osw.write(password + "\n");
		osw.flush();
	}

	protected void copyFromSudo(String header, InputStream sudo)
			throws Exception {
		int b = '\n';
		while (true) {
			if (b == '\n')
				out.print(header);
			b = sudo.read();
			if (b == -1)
				break;
			out.write(b);
			out.flush();
		}
		sudo.close();
	}
}
