/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.identity;

import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.TavernaServerImpl.JMX_ROOT;
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
import javax.jdo.annotations.PersistenceAware;

import org.apache.commons.logging.Log;
import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.taverna.server.master.utils.JDOSupport;

/**
 * The bean class that is responsible for managing the users in the database.
 * 
 * @author Donal Fellows
 */
@PersistenceAware
@ManagedResource(objectName = JMX_ROOT + "Users", description = "The user database.")
public class UserStore extends JDOSupport<User> implements UserDetailsService {
	/** The logger for the user store. */
	private static final Log log = getLog("Taverna.Server.UserDB");

	public UserStore() {
		super(User.class);
	}

	private Map<String, BootstrapUserInfo> base = new HashMap<String, BootstrapUserInfo>();
	private String defLocalUser;
	private PasswordEncoder encoder;

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
		u.setEncodedPassword(encoder.encodePassword(password, u.getUsername()));
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
		if (base == null || base.isEmpty()) {
			log.warn("no baseline user collection");
			return;
		}
		if (!getUsers().isEmpty()) {
			log.info("using existing users from database");
			return;
		}
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
	}

	/**
	 * List the currently-known account names.
	 * 
	 * @return A list of users in the database. Note that this is a snapshot.
	 */
	@WithinSingleTransaction
	@ManagedAttribute(description = "The list of server accounts known about.", currencyTimeLimit = 30)
	public List<String> getUserNames() {
		return getUsers();
	}

	/**
	 * Get a particular user's description.
	 * 
	 * @param userName
	 *            The username to look up.
	 * @return A <i>copy</i> of the user description.
	 */
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
	@WithinSingleTransaction
	public List<UserDetails> listUsers() {
		ArrayList<UserDetails> result = new ArrayList<UserDetails>();
		for (String id : getUsers())
			result.add(detach(getById(id)));
		return result;
	}

	/**
	 * Create a new user account; the account will be disabled and
	 * non-administrative by default. Does not create any underlying system
	 * account.
	 * 
	 * @param username
	 *            The username to create.
	 * @param password
	 *            The password to use.
	 * @param coupleLocalUsername
	 *            Whether to set the local user name to the 'main' one.
	 */
	@WithinSingleTransaction
	@ManagedOperation(description = "Create a new user account; the account will be disabled and non-administrative by default. Does not create any underlying system account.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to create."),
			@ManagedOperationParameter(name = "password", description = "The password to use."),
			@ManagedOperationParameter(name = "coupleLocalUsername", description = "Whether to set the local user name to the 'main' one.") })
	public void addUser(String username, String password,
			boolean coupleLocalUsername) {
		if (username.matches(".*[^a-zA-Z0-9].*"))
			throw new IllegalArgumentException(
					"bad user name; must be pure alphanumeric");
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
	}

	/**
	 * Set or clear whether this account is enabled. Disabled accounts cannot be
	 * used to log in.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param enabled
	 *            Whether to enable the account.
	 */
	@WithinSingleTransaction
	@ManagedOperation(description = "Set or clear whether this account is enabled. Disabled accounts cannot be used to log in.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "enabled", description = "Whether to enable the account.") })
	public void setUserEnabled(String username, boolean enabled) {
		User u = getById(username);
		if (u != null) {
			u.setDisabled(!enabled);
			log.info((enabled ? "enabling" : "disabling") + " user " + username);
		}
	}

	/**
	 * Set or clear the mark on an account that indicates that it has
	 * administrative privileges.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param admin
	 *            Whether the account has admin privileges.
	 */
	@WithinSingleTransaction
	@ManagedOperation(description = "Set or clear the mark on an account that indicates that it has administrative privileges.")
	@ManagedOperationParameters({
			@ManagedOperationParameter(name = "username", description = "The username to adjust."),
			@ManagedOperationParameter(name = "admin", description = "Whether the account has admin privileges.") })
	public void setUserAdmin(String username, boolean admin) {
		User u = getById(username);
		if (u != null) {
			u.setAdmin(admin);
			log.info((admin ? "enabling" : "disabling") + " user " + username
					+ " admin status");
		}
	}

	/**
	 * Change the password for an account.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param password
	 *            The new password to use.
	 */
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
		}
	}

	/**
	 * Change what local system account to use for a server account.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param localUsername
	 *            The new local user account use.
	 */
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
		}
	}

	/**
	 * Delete a server account. The underlying system account is not modified.
	 * 
	 * @param username
	 *            The username to delete.
	 */
	@WithinSingleTransaction
	@ManagedOperation(description = "Delete a server account. The underlying system account is not modified.")
	@ManagedOperationParameters(@ManagedOperationParameter(name = "username", description = "The username to delete."))
	public void deleteUser(String username) {
		delete(getById(username));
		log.info("deleting user " + username);
	}

	@Override
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
			u.setEncodedPassword(encoder.encodePassword(pass, user));
			u.setDisabled(false);
			return u;
		}
	}
}
