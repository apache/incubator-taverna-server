/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static java.util.Collections.emptyList;
import static org.taverna.server.master.common.Namespaces.SERVER;
import static org.taverna.server.master.common.Namespaces.XLINK;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.rest.ContentTypes.JSON;
import static org.taverna.server.master.rest.ContentTypes.TEXT;
import static org.taverna.server.master.rest.ContentTypes.XML;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.CREDS;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.ONE_CRED;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.ONE_PERM;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.ONE_TRUST;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.OWNER;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.PERMS;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.ROOT;
import static org.taverna.server.master.rest.TavernaServerSecurityREST.PathNames.TRUSTS;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCredentialException;

/**
 * Manages the security of the workflow run. In general, only the owner of a run
 * may access this resource. Many of these security-related resources may only
 * be changed before the run is set to operating.
 * 
 * @author Donal Fellows
 */
@RolesAllowed(USER)
@Description("Manages the security of the workflow run. In general, only the "
		+ "owner of a run may access this resource.")
public interface TavernaServerSecurityREST {
	interface PathNames {
		final String ROOT = "/";
		final String OWNER = "owner";
		final String CREDS = "credentials";
		final String ONE_CRED = CREDS + "/{id}";
		final String TRUSTS = "trusts";
		final String ONE_TRUST = TRUSTS + "/{id}";
		final String PERMS = "permissions";
		final String ONE_PERM = PERMS + "/{id}";
	}

	/**
	 * Gets a description of the security information supported by the workflow
	 * run.
	 * 
	 * @param ui
	 *            About the URI used to access this resource.
	 * @return A description of the security information.
	 */
	@GET
	@Path(ROOT)
	@Produces({ XML, JSON })
	@Description("Gives a description of the security information supported "
			+ "by the workflow run.")
	@Nonnull
	Descriptor describe(@Nonnull @Context UriInfo ui);

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ROOT)
	@Description("Produces the description of the run security.")
	Response descriptionOptions();

	/**
	 * Gets the identity of who owns the workflow run.
	 * 
	 * @return The name of the owner of the run.
	 */
	@GET
	@Path(OWNER)
	@Produces(TEXT)
	@Description("Gives the identity of who owns the workflow run.")
	@Nonnull
	String getOwner();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(OWNER)
	@Description("Produces the description of the run owner.")
	Response ownerOptions();

	/*
	 * @PUT @Path("/") @Consumes(ContentTypes.BYTES) @CallCounted @Nonnull
	 * public void set(@Nonnull InputStream contents, @Nonnull @Context UriInfo
	 * ui);
	 */

	/**
	 * @return A list of credentials supplied to this workflow run.
	 */
	@GET
	@Path(CREDS)
	@Produces({ XML, JSON })
	@Description("Gives a list of credentials supplied to this workflow run.")
	@Nonnull
	CredentialList listCredentials();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(CREDS)
	@Description("Produces the description of the run credentials' operations.")
	Response credentialsOptions();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ONE_CRED)
	@Description("Produces the description of one run credential's operations.")
	Response credentialOptions(@PathParam("id") String id);

	/**
	 * Describe a particular credential.
	 * 
	 * @param id
	 *            The id of the credential to fetch.
	 * @return The description of the credential.
	 * @throws NoCredentialException
	 *             If the credential doesn't exist.
	 */
	@GET
	@Path(ONE_CRED)
	@Produces({ XML, JSON })
	@Description("Describes a particular credential.")
	@Nonnull
	CredentialHolder getParticularCredential(@Nonnull @PathParam("id") String id)
			throws NoCredentialException;

	/**
	 * Update a particular credential.
	 * 
	 * @param id
	 *            The id of the credential to update.
	 * @param c
	 *            The details of the credential to use in the update.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return Description of the updated credential.
	 * @throws InvalidCredentialException
	 *             If the credential description isn't valid.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@PUT
	@Path(ONE_CRED)
	@Consumes({ XML, JSON })
	@Produces({ XML, JSON })
	@Description("Updates a particular credential.")
	@Nonnull
	CredentialHolder setParticularCredential(
			@Nonnull @PathParam("id") String id, @Nonnull CredentialHolder c,
			@Nonnull @Context UriInfo ui) throws InvalidCredentialException,
			BadStateChangeException;

	/**
	 * Adds a new credential.
	 * 
	 * @param c
	 *            The details of the credential to create.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return Description of the created credential.
	 * @throws InvalidCredentialException
	 *             If the credential description isn't valid.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@POST
	@Path(CREDS)
	@Consumes({ XML, JSON })
	@Description("Creates a new credential.")
	@Nonnull
	Response addCredential(@Nonnull CredentialHolder c,
			@Nonnull @Context UriInfo ui) throws InvalidCredentialException,
			BadStateChangeException;

	/**
	 * Deletes all credentials associated with a run.
	 * 
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return A characterisation of a successful delete.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@DELETE
	@Path(CREDS)
	@Description("Deletes all credentials.")
	@Nonnull
	Response deleteAllCredentials(@Nonnull @Context UriInfo ui)
			throws BadStateChangeException;

	/**
	 * Deletes one credential associated with a run.
	 * 
	 * @param id
	 *            The identity of the credential to delete.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return A characterisation of a successful delete.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@DELETE
	@Path(ONE_CRED)
	@Description("Deletes a particular credential.")
	@Nonnull
	Response deleteCredential(@Nonnull @PathParam("id") String id,
			@Nonnull @Context UriInfo ui) throws BadStateChangeException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(TRUSTS)
	@Description("Produces the description of the run trusted certificates' "
			+ "operations.")
	Response trustsOptions();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ONE_TRUST)
	@Description("Produces the description of one run trusted certificate's "
			+ "operations.")
	Response trustOptions(@PathParam("id") String id);

	/**
	 * @return A list of trusted identities supplied to this workflow run.
	 */
	@GET
	@Path(TRUSTS)
	@Produces({ XML, JSON })
	@Description("Gives a list of trusted identities supplied to this "
			+ "workflow run.")
	@Nonnull
	TrustList listTrusted();

	/**
	 * Describe a particular trusted identity.
	 * 
	 * @param id
	 *            The id of the trusted identity to fetch.
	 * @return The description of the trusted identity.
	 * @throws NoCredentialException
	 *             If the trusted identity doesn't exist.
	 */
	@GET
	@Path(ONE_TRUST)
	@Produces({ XML, JSON })
	@Description("Describes a particular trusted identity.")
	@Nonnull
	Trust getParticularTrust(@Nonnull @PathParam("id") String id)
			throws NoCredentialException;

	/**
	 * Update a particular trusted identity.
	 * 
	 * @param id
	 *            The id of the trusted identity to update.
	 * @param t
	 *            The details of the trusted identity to use in the update.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return Description of the updated trusted identity.
	 * @throws InvalidCredentialException
	 *             If the trusted identity description isn't valid.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@PUT
	@Path(ONE_TRUST)
	@Consumes({ XML, JSON })
	@Produces({ XML, JSON })
	@Description("Updates a particular trusted identity.")
	@Nonnull
	Trust setParticularTrust(@Nonnull @PathParam("id") String id,
			@Nonnull Trust t, @Nonnull @Context UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException;

	/**
	 * Adds a new trusted identity.
	 * 
	 * @param t
	 *            The details of the trusted identity to create.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return Description of the created trusted identity.
	 * @throws InvalidCredentialException
	 *             If the trusted identity description isn't valid.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@POST
	@Path(TRUSTS)
	@Consumes({ XML, JSON })
	@Description("Adds a new trusted identity.")
	@Nonnull
	Response addTrust(@Nonnull Trust t, @Nonnull @Context UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException;

	/**
	 * Deletes all trusted identities associated with a run.
	 * 
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return A characterisation of a successful delete.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@DELETE
	@Path(TRUSTS)
	@Description("Deletes all trusted identities.")
	@Nonnull
	Response deleteAllTrusts(@Nonnull @Context UriInfo ui)
			throws BadStateChangeException;

	/**
	 * Deletes one trusted identity associated with a run.
	 * 
	 * @param id
	 *            The identity of the trusted identity to delete.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return A characterisation of a successful delete.
	 * @throws BadStateChangeException
	 *             If the workflow run is not in the initialising state.
	 */
	@DELETE
	@Path(ONE_TRUST)
	@Description("Deletes a particular trusted identity.")
	@Nonnull
	Response deleteTrust(@Nonnull @PathParam("id") String id,
			@Nonnull @Context UriInfo ui) throws BadStateChangeException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(PERMS)
	@Description("Produces the description of the run permissions' operations.")
	Response permissionsOptions();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ONE_PERM)
	@Description("Produces the description of one run permission's operations.")
	Response permissionOptions(@PathParam("id") String id);

	/**
	 * @return A list of (non-default) permissions associated with this workflow
	 *         run.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 */
	@GET
	@Path(PERMS)
	@Produces({ XML, JSON })
	@Description("Gives a list of all non-default permissions associated with "
			+ "the enclosing workflow run. By default, nobody has any access "
			+ "at all except for the owner of the run.")
	@Nonnull
	PermissionsDescription describePermissions(@Nonnull @Context UriInfo ui);

	/**
	 * Describe the particular permission granted to a user.
	 * 
	 * @param id
	 *            The name of the user whose permissions are to be described.
	 * @return The permission they are granted.
	 */
	@GET
	@Path(ONE_PERM)
	@Produces(TEXT)
	@Description("Describes the permission granted to a particular user.")
	@Nonnull
	Permission describePermission(@Nonnull @PathParam("id") String id);

	/**
	 * Update the permission granted to a user.
	 * 
	 * @param id
	 *            The name of the user whose permissions are to be updated. Note
	 *            that the owner always has full permissions.
	 * @param perm
	 *            The permission level to set.
	 * @return The permission level that has actually been set.
	 */
	@PUT
	@Consumes(TEXT)
	@Produces(TEXT)
	@Path(ONE_PERM)
	@Description("Updates the permissions granted to a particular user.")
	@Nonnull
	Permission setPermission(@Nonnull @PathParam("id") String id,
			@Nonnull Permission perm);

	/**
	 * Delete the permissions associated with a user, which restores them to the
	 * default (no access unless they are the owner or have admin privileges).
	 * 
	 * @param id
	 *            The name of the user whose permissions are to be revoked.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return An indication that the delete has been successful (or not).
	 */
	@DELETE
	@Path(ONE_PERM)
	@Description("Deletes (by resetting to default) the permissions "
			+ "associated with a particular user.")
	@Nonnull
	Response deletePermission(@Nonnull @PathParam("id") String id,
			@Nonnull @Context UriInfo ui);

	/**
	 * Manufacture a permission setting for a previously-unknown user.
	 * 
	 * @param desc
	 *            A description of the name of the user and the permission level
	 *            to grant them.
	 * @param ui
	 *            Information about the URI used to access this resource.
	 * @return An indication that the create has been successful (or not).
	 */
	@POST
	@Path(PERMS)
	@Consumes({ XML, JSON })
	@Description("Creates a new assignment of permissions to a particular user.")
	@Nonnull
	Response makePermission(@Nonnull PermissionDescription desc,
			@Nonnull @Context UriInfo ui);

	/**
	 * A description of the security resources associated with a workflow run.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "securityDescriptor")
	@XmlType(name = "SecurityDescriptor")
	public static final class Descriptor extends VersionedElement {
		/** The identity of the owner of the enclosing workflow run. */
		@XmlElement
		public String owner;
		/** Where to get the permissions on the run. */
		@XmlElement
		public Uri permissions;

		/** Characterisation of the credentials attached to the run. */
		@XmlElement
		public Credentials credentials;
		/** Characterisation of the trusted certificates attached to the run. */
		@XmlElement
		public Trusts trusts;

		public Descriptor() {
		}

		/**
		 * Initialise a description of the security context.
		 * 
		 * @param ub
		 *            How to build URIs.
		 * @param owner
		 *            Who owns the context.
		 * @param credential
		 *            The credentials associated with the context.
		 * @param trust
		 *            The trusted certificates associated with the context.
		 */
		public Descriptor(@Nonnull UriBuilder ub, @Nonnull String owner,
				@Nonnull Credential[] credential, @Nonnull Trust[] trust) {
			super(true);
			this.owner = owner;
			this.permissions = new Uri(ub, PERMS);
			this.credentials = new Credentials(new Uri(ub, CREDS).ref,
					credential);
			this.trusts = new Trusts(new Uri(ub, TRUSTS).ref, trust);
		}

		/**
		 * A description of credentials associated with a workflow run.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "CredentialCollection")
		public static final class Credentials {
			/** Reference to the collection of credentials */
			@XmlAttribute(name = "href", namespace = XLINK)
			@XmlSchemaType(name = "anyURI")
			public URI href;
			/** Descriptions of the credentials themselves. */
			@XmlElement
			public List<CredentialHolder> credential = new ArrayList<>();

			public Credentials() {
			}

			/**
			 * Initialise a description of the credentials.
			 * 
			 * @param uri
			 *            the URI of the collection.
			 * @param credential
			 *            The credentials in the collection.
			 */
			public Credentials(@Nonnull URI uri,
					@Nonnull Credential[] credential) {
				this.href = uri;
				for (Credential c : credential)
					this.credential.add(new CredentialHolder(c));
			}
		}

		/**
		 * A description of trusted certificates associated with a workflow run.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "TrustCollection")
		public static final class Trusts {
			/** Reference to the collection of trusted certs */
			@XmlAttribute(name = "href", namespace = XLINK)
			@XmlSchemaType(name = "anyURI")
			public URI href;
			/** Descriptions of the trusted certs themselves. */
			@XmlElement
			public Trust[] trust;

			public Trusts() {
			}

			/**
			 * Initialise a description of the trusted certificates.
			 * 
			 * @param uri
			 *            the URI of the collection.
			 * @param trust
			 *            The trusted certificates in the collection.
			 */
			public Trusts(@Nonnull URI uri, @Nonnull Trust[] trust) {
				this.href = uri;
				this.trust = trust.clone();
			}
		}
	}

	/**
	 * A container for a credential, used to work around issues with type
	 * inference in CXF's REST service handling and JAXB.
	 * 
	 * @see Credential.KeyPair
	 * @see Credential.Password
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "credential")
	@XmlType(name = "Credential")
	public static final class CredentialHolder {
		/**
		 * The credential inside this holder.
		 */
		@XmlElements({
				@XmlElement(name = "keypair", namespace = SERVER, type = Credential.KeyPair.class, required = true),
				@XmlElement(name = "userpass", namespace = SERVER, type = Credential.Password.class, required = true) })
		public Credential credential;

		public CredentialHolder() {
		}

		public CredentialHolder(Credential credential) {
			this.credential = credential;
		}

		/**
		 * Convenience accessor function.
		 * 
		 * @return The keypair credential held in this holder.
		 */
		@XmlTransient
		public Credential.KeyPair getKeypair() {
			return (Credential.KeyPair) this.credential;
		}

		/**
		 * Convenience accessor function.
		 * 
		 * @return The userpass credential held in this holder.
		 */
		@XmlTransient
		public Credential.Password getUserpass() {
			return (Credential.Password) this.credential;
		}
	}

	/**
	 * A simple list of credential descriptions.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "credentials")
	public static final class CredentialList extends VersionedElement {
		/** The descriptions of the credentials */
		@XmlElement
		@Nonnull
		public List<CredentialHolder> credential = new ArrayList<>();

		public CredentialList() {
		}

		/**
		 * Initialise the list of credentials.
		 * 
		 * @param credential
		 *            The descriptions of individual credentials.
		 */
		public CredentialList(@Nonnull Credential[] credential) {
			super(true);
			for (Credential c : credential)
				this.credential.add(new CredentialHolder(c));
		}
	}

	/**
	 * A simple list of trusted certificate descriptions.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "trustedIdentities")
	public static final class TrustList extends VersionedElement {
		/** The descriptions of the trusted certificates */
		@XmlElement
		public Trust[] trust;

		public TrustList() {
		}

		/**
		 * Initialise the list of trusted certificates.
		 * 
		 * @param trust
		 *            The descriptions of individual certificates.
		 */
		public TrustList(@Nonnull Trust[] trust) {
			super(true);
			this.trust = trust.clone();
		}
	}

	/**
	 * A description of the permissions granted to others by the owner of a
	 * workflow run.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "permissionsDescriptor")
	public static class PermissionsDescription extends VersionedElement {
		/**
		 * A description of the permissions granted to one user by the owner of
		 * a workflow run.
		 * 
		 * @author Donal Fellows
		 */
		@XmlRootElement(name = "userPermission")
		public static class LinkedPermissionDescription extends Uri {
			/** Who is this granted to? */
			@XmlElement
			public String userName;
			/** What are they granted? */
			@XmlElement
			public Permission permission;

			public LinkedPermissionDescription() {
			}

			/**
			 * Initialise a description of one user's permissions.
			 * 
			 * @param ub
			 *            How to build the URI to this permission. Already
			 *            secured.
			 * @param userName
			 *            Who this relates to.
			 * @param permission
			 *            What permission is granted.
			 * @param strings
			 *            Parameters to the URI builder.
			 */
			LinkedPermissionDescription(@Nonnull UriBuilder ub,
					@Nonnull String userName, @Nonnull Permission permission,
					String... strings) {
				super(ub, strings);
				this.userName = userName;
				this.permission = permission;
			}
		}

		/** List of descriptions of permissions. */
		@XmlElement
		public List<LinkedPermissionDescription> permission;

		public PermissionsDescription() {
			permission = emptyList();
		}

		/**
		 * Initialise the description of a collection of permissions.
		 * 
		 * @param ub
		 *            How to build URIs to this collection. Must have already
		 *            been secured.
		 * @param permissionMap
		 *            The permissions to describe.
		 */
		public PermissionsDescription(@Nonnull UriBuilder ub,
				@Nonnull Map<String, Permission> permissionMap) {
			permission = new ArrayList<>();
			List<String> userNames = new ArrayList<>(permissionMap.keySet());
			Collections.sort(userNames);
			for (String user : userNames)
				permission.add(new LinkedPermissionDescription(ub, user,
						permissionMap.get(user), user));
		}
	}

	/**
	 * An instruction to update the permissions for a user.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "permissionUpdate")
	public static class PermissionDescription {
		/** Who to set the permission for? */
		@XmlElement
		public String userName;
		/** What permission to grant them? */
		@XmlElement
		public Permission permission;
	}
}