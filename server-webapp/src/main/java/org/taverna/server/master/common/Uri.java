package org.taverna.server.master.common;

import static org.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * A class that makes it simpler to work with an element with a {@link URI} in
 * an <tt>href</tt> attribute. Done with JAXB.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "Location")
public class Uri {
	/**
	 * This type is characterised by an attribute that is the reference to some
	 * other element.
	 */
	@XmlAttribute(name = "href", namespace = XLINK)
	public URI ref;

	/** Make a reference that points nowhere. */
	public Uri() {
	}

	/**
	 * Make a reference to the given location.
	 * 
	 * @param ref
	 *            Where to point to.
	 */
	public Uri(URI ref) {
		this.ref = ref;
	}

	/**
	 * Make a reference from the factory with the given parameters.
	 * 
	 * @param ub
	 *            The configured factory.
	 * @param strings
	 *            The parameters to the factory.
	 */
	public Uri(UriBuilder ub, String... strings) {
		ref = ub.build((Object[]) strings);
	}

	/**
	 * Make a reference from the factory with the given parameters.
	 * 
	 * @param ui
	 *            The factory factory.
	 * @param path
	 *            The path to configure the factory with.
	 * @param strings
	 *            The parameters to the factory.
	 */
	public Uri(UriInfo ui, String path, String... strings) {
		this(ui.getAbsolutePathBuilder().path(path), strings);
	}
}