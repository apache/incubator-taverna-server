package org.taverna.server.master.identity;

import static org.taverna.server.master.TavernaServerImpl.JMX_ROOT;
import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.identity.AuthorityDerivedIDMapper.DEFAULT_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.jdo.annotations.PersistenceAware;

import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.taverna.server.master.utils.JDOSupport;

@PersistenceAware
@ManagedResource(objectName = JMX_ROOT + "Users", description = "The user database.")
public class UserStore extends JDOSupport<User> implements UserDetailsService {
	public UserStore() {
		super(User.class);
	}

	private Map<String, UserDetails> base = new HashMap<String, UserDetails>();
	private String defLocalUser;

	public void setBaselineUserProperties(Properties props) {
		UserAttributeEditor configAttribEd = new UserAttributeEditor();

		for (Object name : props.keySet()) {
			String username = (String) name;
			String value = props.getProperty(username);

			// Convert value to a password, enabled setting, and list of granted
			// authorities
			configAttribEd.setAsText(value);

			UserAttribute attr = (UserAttribute) configAttribEd.getValue();

			// Make a user object, assuming the properties were properly
			// provided
			if (attr != null && attr.isEnabled()) {
				base.put(username,
						new org.springframework.security.core.userdetails.User(
								username, attr.getPassword(), true, true, true,
								true, attr.getAuthorities()));
			}
		}
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
		if (base == null || base.isEmpty() || !getUsers().isEmpty())
			return;
		for (String username : base.keySet()) {
			UserDetails ud = base.get(username);
			if (ud == null)
				continue;
			User u = new User();
			boolean realUser = false;
			for (GrantedAuthority ga : ud.getAuthorities()) {
				String a = ga.getAuthority();
				if (a.startsWith(DEFAULT_PREFIX))
					u.setLocalUsername(a.substring(DEFAULT_PREFIX.length()));
				else if (a.equals(USER))
					realUser = true;
				else if (a.equals(ADMIN))
					u.setAdmin(true);
			}
			if (!realUser)
				continue;
			u.setUsername(username);
			u.setPassword(ud.getPassword());
			u.setDisabled(!ud.isEnabled());
			persist(u);
		}
		base.clear();
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
		info.put("pass", u.getPassword());
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
		u.setPassword(password);
		if (coupleLocalUsername)
			u.setLocalUsername(username);
		else
			u.setLocalUsername(defLocalUser);
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
		u.setDisabled(!enabled);
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
		u.setAdmin(admin);
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
		u.setPassword(password);
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
		u.setLocalUsername(localUsername);
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
	}

	@Override
	@WithinSingleTransaction
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		UserDetails ud = base.get(username);
		if (ud != null)
			return ud;
		User u;
		try {
			u = detach(getById(username));
		} catch (Exception ex) {
			throw new UsernameNotFoundException("who are you?", ex);
		}
		if (u != null)
			return u;
		throw new UsernameNotFoundException("who are you?");
	}

}
