/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.utils;

import static org.taverna.server.master.utils.Contextualizer.SUBSTITUAND;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.annotation.PreDestroy;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Add some awareness of the context so that we can locate databases internally
 * to the webapp.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings("IS2_INCONSISTENT_SYNC")
public class WebappAwareDataSource extends BasicDataSource {
	Log log = LogFactory.getLog("Taverna.Server.Utils");
	private transient boolean init;
	private Contextualizer ctxt;
	private String shutdownUrl;

	@Required
	public void setContextualizer(Contextualizer ctxt) {
		this.ctxt = ctxt;
	}

	/**
	 * A JDBC connection URL to use on shutting down the database. If not set,
	 * do nothing special.
	 * 
	 * @param url
	 */
	public void setShutdownUrl(String url) {
		shutdownUrl = url;
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

	@PreDestroy
	void realClose() {
		try {
			close();
		} catch (SQLException e) {
			log.warn("problem shutting down DB connection", e);
		}
		try {
			if (shutdownUrl != null)
				DriverManager.getConnection(ctxt.contextualize(shutdownUrl));
		} catch (SQLException e) {
			// Expected; ignore it
		}
		log = null;
	}
}
