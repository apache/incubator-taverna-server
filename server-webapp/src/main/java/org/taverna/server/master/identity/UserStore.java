package org.taverna.server.master.identity;

import static org.springframework.security.core.userdetails.memory.UserMapEditor.addUsersFromProperties;
import static org.taverna.server.master.TavernaServerImpl.JMX_ROOT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jdo.annotations.PersistenceAware;

import org.springframework.dao.DataAccessException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserMap;
import org.taverna.server.master.utils.JDOSupport;

@PersistenceAware
@ManagedResource(objectName = JMX_ROOT + "Users", description = "The user database.")
public class UserStore extends JDOSupport<User> implements UserDetailsService {
	public UserStore() {
		super(User.class);
	}

	private UserMap baseline;
	private String defLocalUser;

	public void setBaselineUserProperties(Properties props) {
		baseline = new UserMap();
		baseline = addUsersFromProperties(baseline, props);
	}

	public void setDefaultLocalUser(String defLocalUser) {
		this.defLocalUser = defLocalUser;
	}

	@WithinSingleTransaction
	@ManagedAttribute
	public List<String> getUserNames() {
		@SuppressWarnings("unchecked")
		List<String> ids = (List<String>) namedQuery("users").execute();
		return ids;
	}

	@WithinSingleTransaction
	public User getUser(String userName) {
		return detach(getById(userName));
	}

	@WithinSingleTransaction
	@ManagedOperation
	public Map<String, String> getUserInfo(String userName) {
		User u = getById(userName);
		Map<String, String> info = new HashMap<String, String>();
		info.put("name", u.getUsername());
		info.put("pass", u.getPassword());
		info.put("admin", u.isAdmin() ? "1" : "0");
		info.put("enabled", u.isEnabled() ? "1" : "0");
		info.put("localID", u.getLocalUsername());
		return info;
	}

	@WithinSingleTransaction
	public List<UserDetails> listUsers() {
		@SuppressWarnings("unchecked")
		List<String> ids = (List<String>) namedQuery("users").execute();
		ArrayList<UserDetails> result = new ArrayList<UserDetails>();
		for (String id : ids)
			result.add(detach(getById(id)));
		return result;
	}

	@WithinSingleTransaction
	@ManagedOperation
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

	@WithinSingleTransaction
	@ManagedOperation
	public void setUserEnabled(String username, boolean enabled) {
		User u = getById(username);
		u.setDisabled(!enabled);
	}

	@WithinSingleTransaction
	@ManagedOperation
	public void setUserAdmin(String username, boolean admin) {
		User u = getById(username);
		u.setAdmin(admin);
	}

	@WithinSingleTransaction
	@ManagedOperation
	public void setUserPassword(String username, String password) {
		User u = getById(username);
		u.setPassword(password);
	}

	@WithinSingleTransaction
	@ManagedOperation
	public void setUserLocalUser(String username, String localUsername) {
		User u = getById(username);
		u.setLocalUsername(localUsername);
	}

	@WithinSingleTransaction
	@ManagedOperation
	public void deleteUser(String username) {
		delete(getById(username));
	}

	@Override
	@WithinSingleTransaction
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		try {
			if (baseline != null)
				return baseline.getUser(username);
		} catch (UsernameNotFoundException e) {
			// Dropthrough
		}
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
