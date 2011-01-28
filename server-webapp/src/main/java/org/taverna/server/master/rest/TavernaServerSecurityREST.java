/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.rest;

import static java.util.Collections.emptyList;
import static org.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.ext.Description;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCredentialException;

// FIXME document this interface (and its contained helper classes)
public interface TavernaServerSecurityREST {
	@GET
	@Path("/")
	@Produces("application/xml")
	Descriptor describe(@Context UriInfo ui);

	/**
	 * Gets the identity of who owns the workflow run.
	 * 
	 * @return The name of the owner of the run.
	 */
	@GET
	@Path("owner")
	@Produces("text/plain")
	@Description("Gives the identity of who owns the workflow run.")
	String getOwner();

	/*
	 * @PUT
	 * 
	 * @Path("/")
	 * 
	 * @Consumes(APPLICATION_OCTET_STREAM) public void set(InputStream contents,
	 * @Context UriInfo ui);
	 */

	@GET
	@Path("credentials")
	@Produces("application/xml")
	CredentialList listCredentials();

	@GET
	@Path("credentials/{id}")
	@Produces("application/xml")
	Credential getParticularCredential(@PathParam("id") String id)
			throws NoCredentialException;

	@PUT
	@Path("credentials/{id}")
	@Consumes("appplication/xml")
	@Produces("application/xml")
	Credential setParticularCredential(@PathParam("id") String id,
			Credential c, @Context UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException;

	@POST
	@Path("credentials")
	@Consumes("application/xml")
	Response addCredential(Credential c, @Context UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException;

	@DELETE
	@Path("credentials")
	Response deleteAllCredentials(@Context UriInfo ui)
			throws BadStateChangeException;

	@DELETE
	@Path("credentials/{id}")
	Response deleteCredential(@PathParam("id") String id, @Context UriInfo ui)
			throws BadStateChangeException;

	@GET
	@Path("trusts")
	@Produces("application/xml")
	TrustList listTrusted();

	@GET
	@Path("trusts/{id}")
	@Produces("application/xml")
	Trust getParticularTrust(@PathParam("id") String id)
			throws NoCredentialException;

	@PUT
	@Path("trusts/{id}")
	@Consumes("appplication/xml")
	@Produces("application/xml")
	Trust setParticularTrust(@PathParam("id") String id, Trust t,
			@Context UriInfo ui) throws InvalidCredentialException,
			BadStateChangeException;

	@POST
	@Path("trusts")
	@Consumes("application/xml")
	Response addTrust(Trust c, @Context UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException;

	@DELETE
	@Path("trusts")
	Response deleteAllTrusts(@Context UriInfo ui)
			throws BadStateChangeException;

	@DELETE
	@Path("trusts/{id}")
	Response deleteTrust(@PathParam("id") String id, @Context UriInfo ui)
			throws BadStateChangeException;

	@GET
	@Produces({ "application/xml", "application/json" })
	@Path("permissions")
	PermissionsDescription describePermissions(@Context UriInfo ui);

	@GET
	@Produces("text/plain")
	@Path("permissions/{id}")
	Permission describePermission(@PathParam("id") String id);

	@PUT
	@Consumes("text/plain")
	@Produces("text/plain")
	@Path("permissions/{id}")
	Permission setPermission(@PathParam("id") String id, Permission perm);

	@DELETE
	@Path("permissions/{id}")
	Response deletePermission(@PathParam("id") String id, @Context UriInfo ui);

	@POST
	@Consumes("application/xml")
	@Path("permissions")
	Response makePermission(PermissionDescription desc, @Context UriInfo ui);

	@XmlRootElement(name = "securityDescriptor")
	@XmlType(name = "SecurityDescriptor")
	public static final class Descriptor extends VersionedElement {
		@XmlElement
		public String owner;
		@XmlElement
		public Uri permissions;

		@XmlElement
		public Credentials credentials;
		@XmlElement
		public Trusts trusts;

		public Descriptor() {
		}

		public Descriptor(UriBuilder ub, String owner, Credential[] credential,
				Trust[] trust) {
			super(true);
			this.owner = owner;
			this.permissions = new Uri(ub, "permissions");
			this.credentials = new Credentials(ub.build("credentials"),
					credential);
			this.trusts = new Trusts(ub.build("trusts"), trust);
		}

		@XmlType(name = "CredentialCollection")
		public static final class Credentials {
			@XmlAttribute(name = "href", namespace = XLINK)
			public URI href;
			@XmlElement
			public Credential[] credential;

			public Credentials() {
			}

			public Credentials(URI uri, Credential[] credential) {
				this.href = uri;
				this.credential = credential;
			}
		}

		@XmlType(name = "TrustCollection")
		public static final class Trusts {
			@XmlAttribute(name = "href", namespace = XLINK)
			public URI href;
			@XmlElement
			public Trust[] trust;

			public Trusts() {
			}

			public Trusts(URI uri, Trust[] trust) {
				this.href = uri;
				this.trust = trust;
			}
		}
	}

	@XmlRootElement(name = "credentials")
	public static final class CredentialList extends VersionedElement {
		@XmlElement
		public Credential[] credential;

		public CredentialList() {
		}

		public CredentialList(Credential[] credential) {
			super(true);
			this.credential = credential;
		}
	}

	@XmlRootElement(name = "trustedIdentities")
	public static final class TrustList extends VersionedElement {
		@XmlElement
		public Trust[] trust;

		public TrustList() {
		}

		public TrustList(Trust[] trust) {
			super(true);
			this.trust = trust;
		}
	}

	@XmlRootElement(name = "permissionsDescriptor")
	public static class PermissionsDescription extends VersionedElement {
		@XmlRootElement(name = "userPermission")
		public static class LinkedPermissionDescription extends Uri {
			@XmlElement
			public String userName;
			@XmlElement
			public Permission permission;

			public LinkedPermissionDescription() {
			}

			public LinkedPermissionDescription(UriBuilder ub, String userName,
					Permission permission, String... strings) {
				super(ub, strings);
				this.userName = userName;
				this.permission = permission;
			}
		}

		@XmlElement
		public List<LinkedPermissionDescription> permission;

		public PermissionsDescription() {
			permission = emptyList();
		}

		public PermissionsDescription(UriBuilder ub,
				Map<String, Permission> permissionMap) {
			permission = new ArrayList<LinkedPermissionDescription>();
			List<String> userNames = new ArrayList<String>(
					permissionMap.keySet());
			Collections.sort(userNames);
			for (String user : userNames)
				permission.add(new LinkedPermissionDescription(ub, user,
						permissionMap.get(user), user));
		}
	}

	@XmlRootElement(name = "permissionUpdate")
	public static class PermissionDescription {
		@XmlElement
		public String userName;
		@XmlElement
		public Permission permission;
	}
}