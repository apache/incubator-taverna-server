package org.taverna.server.master.identity;

import java.util.List;

/**
 * The API that is exposed by the DAO that exposes user management.
 * 
 * @author Donal Fellows
 * @see User
 */
public interface UserStoreAPI {
	/**
	 * List the currently-known account names.
	 * 
	 * @return A list of users in the database. Note that this is a snapshot.
	 */
	List<String> getUserNames();

	/**
	 * Get a particular user's description.
	 * 
	 * @param userName
	 *            The username to look up.
	 * @return A <i>copy</i> of the user description.
	 */
	User getUser(String userName);

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
	void addUser(String username, String password, boolean coupleLocalUsername);

	/**
	 * Set or clear whether this account is enabled. Disabled accounts cannot be
	 * used to log in.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param enabled
	 *            Whether to enable the account.
	 */
	void setUserEnabled(String username, boolean enabled);

	/**
	 * Set or clear the mark on an account that indicates that it has
	 * administrative privileges.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param admin
	 *            Whether the account has admin privileges.
	 */
	void setUserAdmin(String username, boolean admin);

	/**
	 * Change the password for an account.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param password
	 *            The new password to use.
	 */
	void setUserPassword(String username, String password);

	/**
	 * Change what local system account to use for a server account.
	 * 
	 * @param username
	 *            The username to adjust.
	 * @param localUsername
	 *            The new local user account use.
	 */
	void setUserLocalUser(String username, String localUsername);

	/**
	 * Delete a server account. The underlying system account is not modified.
	 * 
	 * @param username
	 *            The username to delete.
	 */
	void deleteUser(String username);
}
