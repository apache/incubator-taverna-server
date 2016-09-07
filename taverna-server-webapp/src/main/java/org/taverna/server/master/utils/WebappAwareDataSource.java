/*
 */
package org.taverna.server.master.utils;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.lang.Thread.currentThread;
import static java.sql.DriverManager.deregisterDriver;
import static java.sql.DriverManager.getDrivers;
import static org.taverna.server.master.utils.Contextualizer.ROOT_PLACEHOLDER;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.annotation.PreDestroy;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Add some awareness of the context so that we can locate databases internally
 * to the webapp.
 * 
 * @author Donal Fellows
 */
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
				setDriverClassLoader(currentThread().getContextClassLoader());
				String url = getUrl();
				if (url.contains(ROOT_PLACEHOLDER)) {
					String newurl = ctxt.contextualize(url);
					setUrl(newurl);
					log.info("mapped " + url + " to " + newurl);
				} else {
					log.info("did not find " + ROOT_PLACEHOLDER + " in " + url);
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
		dropDriver();
	}

	private void dropDriver() {
		Enumeration<Driver> drivers = getDrivers();
		while (drivers.hasMoreElements()) {
			Driver d = drivers.nextElement();
			if (d.getClass().getClassLoader() == getDriverClassLoader()
					&& d.getClass().getName().equals(getDriverClassName())) {
				try {
					deregisterDriver(d);
				} catch (SQLException e) {
				}
				break;
			}
		}
	}
}
