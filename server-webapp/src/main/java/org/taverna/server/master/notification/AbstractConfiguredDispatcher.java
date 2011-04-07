package org.taverna.server.master.notification;

import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.ServletContextAware;
import org.taverna.server.master.interfaces.MessageDispatcher;
import org.taverna.server.master.interfaces.TavernaRun;

public abstract class AbstractConfiguredDispatcher implements
		MessageDispatcher, ServletContextAware {
	/**
	 * @param prefix
	 *            The prefix to use when looking up bean parameters.
	 */
	public AbstractConfiguredDispatcher(String prefix) {
		this.prefix = prefix;
	}

	private String prefix;
	private Properties properties = new Properties();
	private ServletContext config;
	/** Pre-configured logger. */
	protected Log log = LogFactory.getLog("Taverna.Server.Notification");

	@Override
	public final void setServletContext(ServletContext servletContext) {
		this.config = servletContext;
		reconfigured();
		if (!isAvailable())
			log.warn("incomplete configuration; disabling " + prefix
					+ " notification dispatcher");
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
		reconfigured();
	}

	public Properties getProperties() {
		return properties;
	}

	/**
	 * Get the bean parameter with the given name.
	 * 
	 * @param key
	 *            The key <i>suffix</i> to use when looking up the parameter.
	 * @return The content of the parameter. The empty string is returned if the
	 *         parameter is absent.
	 */
	public String getParam(String key) {
		String k = prefix + "." + key;
		String param = config.getInitParameter(k);
		String source = "ServletConfig";
		if (param == null) {
			param = properties.getProperty(key);
			source = "SpringConfig";
		}
		log.info("configured " + prefix + " dispatcher with param " + k
				+ " as " + param + " (source:" + source + ")");
		return param == null ? "" : param;
	}

	/**
	 * Called to indicate that this bean has been reconfigured with a source of
	 * parameters, so any results of {@link #getParam(String)} should be
	 * refetched.
	 */
	public void reconfigured() {
	}

	@Override
	public void dispatch(TavernaRun ignored, String messageSubject,
			String messageContent, String target) throws Exception {
		dispatch(messageSubject, messageContent, target);
	}

	/**
	 * Dispatch a message to a recipient that doesn't care what produced it.
	 * 
	 * @param messageSubject
	 *            The subject of the message to send.
	 * @param messageContent
	 *            The plain-text content of the message to send.
	 * @param target
	 *            A description of where it is to go.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	public abstract void dispatch(String messageSubject, String messageContent,
			String target) throws Exception;
}
