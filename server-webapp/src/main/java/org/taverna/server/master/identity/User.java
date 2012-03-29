package org.taverna.server.master.identity;

import static org.taverna.server.master.common.Roles.ADMIN;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.identity.AuthorityDerivedIDMapper.DEFAULT_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@PersistenceCapable(schema = "USERS", table = "LIST")
@Query(name = "users", language = "SQL", value = "SELECT id FROM USERS.LIST ORDER BY id", resultClass = String.class)
@XmlRootElement
@XmlType(name = "User", propOrder = {})
public class User implements UserDetails {
	@XmlElement
	@Persistent
	private boolean disabled;
	@XmlElement(name = "username", required = true)
	@Persistent(primaryKey = "true")
	private String id;
	@XmlElement(required = true)
	@Persistent
	private String password;
	@XmlElement
	@Persistent
	private boolean admin;
	@XmlElement
	@Persistent
	private String localUsername;

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
		auths.add(new LiteralGrantedAuthority(USER));
		if (admin)
			auths.add(new LiteralGrantedAuthority(ADMIN));
		if (localUsername != null)
			auths.add(new LiteralGrantedAuthority(DEFAULT_PREFIX
					+ localUsername));
		return auths;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return id;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return !disabled;
	}

	void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	void setUsername(String username) {
		this.id = username;
	}

	void setPassword(String password) {
		this.password = password;
	}

	void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isAdmin() {
		return admin;
	}

	void setLocalUsername(String localUsername) {
		this.localUsername = localUsername;
	}

	public String getLocalUsername() {
		return localUsername;
	}
}

class LiteralGrantedAuthority implements GrantedAuthority {
	private String auth;

	LiteralGrantedAuthority(String auth) {
		this.auth = auth;
	}

	@Override
	public String getAuthority() {
		return auth;
	}
}
