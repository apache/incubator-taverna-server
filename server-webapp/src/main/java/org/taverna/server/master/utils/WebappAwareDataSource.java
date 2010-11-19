package org.taverna.server.master.utils;

import static org.taverna.server.master.TavernaServerImpl.log;
import static org.taverna.server.master.utils.Contextualizer.SUBSTITUAND;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * Add some awareness of the context so that we can locate databases internally
 * to the webapp.
 * 
 * @author Donal Fellows
 */
public class WebappAwareDataSource extends BasicDataSource {
	private transient boolean init;
	private Contextualizer ctxt;
	public void setContextualizer(Contextualizer ctxt) {
		this.ctxt = ctxt;
	}

	private void doInit() {
		synchronized (this) {
			if (!init) {
				String url = getUrl();
				if (url.contains(SUBSTITUAND)) {
					String newurl = ctxt.contextualize(url);
					setUrl(newurl);
					log.info("mapped " + url + " to " + newurl);
				} else {
					log.info("did not find " + SUBSTITUAND + " in " + url);
				}
				init = true;
			}
		}
	}

	// -=-=-=-=-=-=-=-=-=-=- HOOKS -=-=-=-=-=-=-=-=-=-=-

	@Override
	public Connection getConnection() throws SQLException {
		doInit();
		return super.getConnection();
	}

	@Override
	public void setLogWriter(PrintWriter pw) throws SQLException {
		doInit();
		super.setLogWriter(pw);
	}

	@Override
	public void setLoginTimeout(int num) throws SQLException {
		doInit();
		super.setLoginTimeout(num);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		doInit();
		return super.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		doInit();
		return super.getLoginTimeout();
	}
}
