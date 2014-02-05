/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.identity;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.TavernaServer.JMX_ROOT;
import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.defaults.Default.AUTHORITY_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jdo.annotations.PersistenceAware;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
import org.taverna.server.master.utils.JDOSupport;

/**
 * The bean class that is responsible for managing the users in the database.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
@ManagedResource(objectName = JMX_ROOT + "Users", description = "The user database.")
public class UserStore extends JDOSupport<User> implements UserDetailsService,
		UserStoreAPI {
	/** The logger for the user store. */
	private static Log log = getLog("Taverna.Server.UserDB");

	public UserStore() {
		super(User.class);
	}

	@PreDestroy
	void closeLog() {
		log = null;
	}

	private Map<String, BootstrapUserInfo> base = new HashMap<String, BootstrapUserInfo>();
	private String defLocalUser;
	private PasswordEncoder encoder;
	private volatile int epoch;

	/**
	 * Install the encoder that will be used to turn a plaintext password into
	 * something that it is safe to store in the database.
	 * 
	 * @param encoder
	 *            The password encoder bean to install.
	 */
	public void setEncoder(PasswordEncoder encoder) {
		this.encoder = encoder;
	}

	public void setBaselineUserProperties(Properties props) {
		UserAttributeEditor parser = new UserAttributeEditor();

		for (Object name : props.keySet()) {
			String username = (String) name;
			String value = props.getProperty(username);

			// Convert value to a password, enabled setting, and list of granted
			// authorities
			parser.setAsText(value);

			UserAttribute attr = (UserAttribute) parser.getValue();
			if (attr != null && attr.isEnabled())
				base.put(username, new BootstrapUserInfo(username, attr));
		}
	}

	private void installPassword(User u, String password) {
		u.setEncodedPassword(encoder.encode(password));
	}

	public void setDefaultLocalUser(String defLocalUser) {
		this.defLocalUser = defLocalUser;
	}

	@SuppressWarnings("unchecked")
	private List<String> getUsers() {
		return (List<String>) namedQuery("users").execute();
	}

	@WithinSingleTransaction
	@PostConstruct
	void initDB() {
		if (base == null || base.isEmpty())
			log.warn("no baseline user collection");
		else if (!getUsers().isEmpty())
			log.info("using existing users from database");
		else
			for (String username : base.keySet()) {
				BootstrapUserInfo ud = base.get(username);
				if (ud == null)
					continue;
				User u = ud.get(encoder);
				if (u == null)
					continue;
				log.info("bootstrapping user " + username + " in the database");
				persist(u);
			}
		base = null;
		epoch++;
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedAttribute(description = "The list of server accounts known about.", currencyTimeLimit = 30)
	public List<String> getUserNames() {
		return getUsers();
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	public User getUser(String userName) {
		return detach(getById(userName));
	}

	/**
	 * Get information about a server account.
	 * 
	 * @param userName
	 *            The username to look up.
	 * @return A description map intended for use by a server admin over JMX.
	 */
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Get information about a server account.")
	@ManagedOperationParameters(@ManagedOperationParameter(name = "userName", description = "The username to look up."))
	public Map<String, String> getUserInfo(String userName) {
		User u = getById(userName);
		Map<String, String> info = new HashMap<String, String>();
		info.put("name", u.getUsername());
		info.put("admin", u.isAdmin() ? "yes" : "no");
		info.put("enabled", u.isEnabled() ? "yes" : "no");
		info.put("localID", u.getLocalUsername());
		return info;
	}

	/**
	 * Get a list of all the users in the database.
	 * 
	 * @return A list of user details, <i>copied</i> out of the database.
	 */
	@PerfLogged
	@WithinSingleTransaction
	public List<UserDetails> listUsers() {
		ArrayList<UserDetails> result = new ArrayList<UserDetails>();
		for (String id : getUsers())
			result.add(detach(getById(id)));
		return result;
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Create a new user account; the account will be disabled and "
			+ "non-administrative by default. Does not create any underlying system account.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to create."),
			@ManagedOperationParameter(name = "password", description = "The password to use."),
			@ManagedOperationParameter(name = "coupleLocalUsername", description = "Whether to set the local user name to the 'main' one.") })
	public void addUser(String username, String password,
			boolean coupleLocalUsername) {
		if (username.matches(".*[^a-zA-Z0-9].*"))
			throw new IllegalArgumentException(
					"bad user name; must be pure alphanumeric");
		if (getById(username) != null)
			throw new IllegalArgumentException("user name already exists");
		User u = new User();
		u.setDisabled(true);
		u.setAdmin(false);
		u.setUsername(username);
		installPassword(u, password);
		if (coupleLocalUsername)
			u.setLocalUsername(username);
		else
			u.setLocalUsername(defLocalUser);
		log.info("creating user for " + username);
		persist(u);
		epoch++;
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Set or clear whether this account is enabled. "
			+ "Disabled accounts cannot be used to log in.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "enabled", description = "Whether to enable the account.") })
	public void setUserEnabled(String username, boolean enabled) {
		User u = getById(username);
		if (u != null) {
			u.setDisabled(!enabled);
			log.info((enabled ? "enabling" : "disabling") + " user " + username);
			epoch++;
		}
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Set or clear the mark on an account that indicates "
			+ "that it has administrative privileges.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "admin", description = "Whether the account has admin privileges.") })
	public void setUserAdmin(String username, boolean admin) {
		User u = getById(username);
		if (u != null) {
			u.setAdmin(admin);
			log.info((admin ? "enabling" : "disabling") + " user " + username
					+ " admin status");
			epoch++;
		}
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Change the password for an account.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "password", description = "The new password to use.") })
	public void setUserPassword(String username, String password) {
		User u = getById(username);
		if (u != null) {
			installPassword(u, password);
			log.info("changing password for user " + username);
			epoch++;
		}
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Change what local system account to use for a server account.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "password", description = "The new local user account use.") })
	public void setUserLocalUser(String username, String localUsername) {
		User u = getById(username);
		if (u != null) {
			u.setLocalUsername(localUsername);
			log.info("mapping user " + username + " to local account "
					+ localUsername);
			epoch++;
		}
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	@ManagedOperation(description = "Delete a server account. The underlying "
			+ "system account is not modified.")
	@ManagedOperationParameters(@ManagedOperationParameter(name = "username", description = "The username to delete."))
	public void deleteUser(String username) {
		delete(getById(username));
		log.info("deleting user " + username);
		epoch++;
	}

	@Override
	@PerfLogged
	@WithinSingleTransaction
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		User u;
		if (base != null) {
			log.warn("bootstrap user store still installed!");
			BootstrapUserInfo ud = base.get(username);
			if (ud != null) {
				log.warn("retrieved production credentials for " + username
						+ " from bootstrap store");
				u = ud.get(encoder);
				if (u != null)
					return u;
			}
		}
		try {
			u = detach(getById(username));
		} catch (NullPointerException npe) {
			throw new UsernameNotFoundException("who are you?");
		} catch (Exception ex) {
			throw new UsernameNotFoundException("who are you?", ex);
		}
		if (u != null)
			return u;
		throw new UsernameNotFoundException("who are you?");
	}

	int getEpoch() {
		return epoch;
	}

	public static class CachedUserStore implements UserDetailsService {
		private int epoch;
		private Map<String, UserDetails> cache = new HashMap<String, UserDetails>();
		private UserStore realStore;

		@Required
		public void setRealStore(UserStore store) {
			this.realStore = store;
		}

		@Override
		@PerfLogged
		public UserDetails loadUserByUsername(String username) {
			int epoch = realStore.getEpoch();
			UserDetails details;
			synchronized (cache) {
				if (epoch != this.epoch) {
					cache.clear();
					this.epoch = epoch;
					details = null;
				} else
					details = cache.get(username);
			}
			if (details == null) {
				details = realStore.loadUserByUsername(username);
				synchronized (cache) {
					cache.put(username, details);
				}
			}
			return details;
		}
	}

	private static class BootstrapUserInfo {
		private String user;
		private String pass;
		private Collection<GrantedAuthority> auth;

		BootstrapUserInfo(String username, UserAttribute attr) {
			user = username;
			pass = attr.getPassword();
			auth = attr.getAuthorities();
		}

		User get(PasswordEncoder encoder) {
			User u = new User();
			boolean realUser = false;
			for (GrantedAuthority ga : auth) {
				String a = ga.getAuthority();
				if (a.startsWith(AUTHORITY_PREFIX))
					u.setLocalUsername(a.substring(AUTHORITY_PREFIX.length()));
				else if (a.equals(USER))
					realUser = true;
				else if (a.equals(ADMIN))
					u.setAdmin(true);
			}
			if (!realUser)
				return null;
			u.setUsername(user);
			u.setEncodedPassword(encoder.encode(pass));
			u.setDisabled(false);
			return u;
		}
	}
}
