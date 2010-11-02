package org.taverna.server.master;

import java.security.Principal;
import java.util.List;

import org.taverna.server.master.interfaces.LocalIdentityMapper;

/**
 * An identity mapper that composes the results from other mappers, using the
 * identity mappers in order until one can provide a non-<tt>null</tt> answer.
 * 
 * @author Donal Fellows.
 */
public class CompositeIDMapper implements LocalIdentityMapper {
	private List<LocalIdentityMapper> mappers;

	/**
	 * @param mappers
	 *            The list of mappers to delegate to. Order is significant.
	 */
	public void setIdentityMappers(List<LocalIdentityMapper> mappers) {
		this.mappers = mappers;
	}

	@Override
	public String getUsernameForPrincipal(Principal user) {
		if (mappers == null)
			return null;
		for (LocalIdentityMapper m : mappers) {
			String u = m.getUsernameForPrincipal(user);
			if (u != null)
				return u;
		}
		return null;
	}
}
