/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.taverna.server.master.common.Status.Initialized;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.TavernaServerImpl.SupportAware;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCredentialException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.TavernaServerSecurityREST;
import org.taverna.server.master.utils.InvocationCounter.CallCounted;

/**
 * RESTful interface to a single workflow run's security settings.
 * 
 * @author Donal Fellows
 */
class RunSecurityREST implements TavernaServerSecurityREST, SecurityBean {
	private TavernaServerSupport support;
	private TavernaSecurityContext context;
	private TavernaRun run;

	@Override
	public void setSupport(TavernaServerSupport support) {
		this.support = support;
	}

	@Override
	public RunSecurityREST connect(TavernaSecurityContext context,
			TavernaRun run) {
		this.context = context;
		this.run = run;
		return this;
	}

	@Override
	@CallCounted
	public Descriptor describe(UriInfo ui) {
		return new Descriptor(ui.getAbsolutePathBuilder().path("{element}"),
				context.getOwner().getName(), context.getCredentials(),
				context.getTrusted());
	}

	@Override
	@CallCounted
	public String getOwner() {
		return context.getOwner().getName();
	}

	@Override
	@CallCounted
	public CredentialList listCredentials() {
		return new CredentialList(context.getCredentials());
	}

	@Override
	@CallCounted
	public CredentialHolder getParticularCredential(String id)
			throws NoCredentialException {
		for (Credential c : context.getCredentials())
			if (c.id.equals(id))
				return new CredentialHolder(c);
		throw new NoCredentialException();
	}

	@Override
	@CallCounted
	public CredentialHolder setParticularCredential(String id,
			CredentialHolder cred, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		Credential c = cred.credential;
		c.id = id;
		c.href = ui.getAbsolutePath().toString();
		context.validateCredential(c);
		context.deleteCredential(c);
		context.addCredential(c);
		return new CredentialHolder(c);
	}

	@Override
	@CallCounted
	public Response addCredential(CredentialHolder cred, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		Credential c = cred.credential;
		c.id = randomUUID().toString();
		URI uri = ui.getAbsolutePathBuilder().path("{id}").build(c.id);
		c.href = uri.toString();
		context.validateCredential(c);
		context.addCredential(c);
		return created(uri).build();
	}

	@Override
	@CallCounted
	public Response deleteAllCredentials(UriInfo ui)
			throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		for (Credential c : context.getCredentials())
			context.deleteCredential(c);
		return noContent().build();
	}

	@Override
	@CallCounted
	public Response deleteCredential(String id, UriInfo ui)
			throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		context.deleteCredential(new Credential.Dummy(id));
		return noContent().build();
	}

	@Override
	@CallCounted
	public TrustList listTrusted() {
		return new TrustList(context.getTrusted());
	}

	@Override
	@CallCounted
	public Trust getParticularTrust(String id) throws NoCredentialException {
		for (Trust t : context.getTrusted())
			if (t.id.equals(id))
				return t;
		throw new NoCredentialException();
	}

	@Override
	@CallCounted
	public Trust setParticularTrust(String id, Trust t, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		t.id = id;
		t.href = ui.getAbsolutePath().toString();
		context.validateTrusted(t);
		context.deleteTrusted(t);
		context.addTrusted(t);
		return t;
	}

	@Override
	@CallCounted
	public Response addTrust(Trust t, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		t.id = randomUUID().toString();
		URI uri = ui.getAbsolutePathBuilder().path("{id}").build(t.id);
		t.href = uri.toString();
		context.validateTrusted(t);
		context.addTrusted(t);
		return created(uri).build();
	}

	@Override
	@CallCounted
	public Response deleteAllTrusts(UriInfo ui) throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		for (Trust t : context.getTrusted())
			context.deleteTrusted(t);
		return noContent().build();
	}

	@Override
	@CallCounted
	public Response deleteTrust(String id, UriInfo ui)
			throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		Trust toDelete = new Trust();
		toDelete.id = id;
		context.deleteTrusted(toDelete);
		return noContent().build();
	}

	@Override
	@CallCounted
	public PermissionsDescription describePermissions(UriInfo ui) {
		Map<String, Permission> perm = new HashMap<String, Permission>();
		for (String u : context.getPermittedReaders())
			perm.put(u, Permission.Read);
		for (String u : context.getPermittedUpdaters())
			perm.put(u, Permission.Update);
		for (String u : context.getPermittedDestroyers())
			perm.put(u, Permission.Destroy);
		return new PermissionsDescription(ui.getAbsolutePathBuilder().path(
				"{id}"), perm);
	}

	@Override
	@CallCounted
	public Permission describePermission(String id) {
		return support.getPermission(context, id);
	}

	@Override
	@CallCounted
	public Permission setPermission(String id, Permission perm) {
		support.setPermission(context, id, perm);
		return support.getPermission(context, id);
	}

	@Override
	@CallCounted
	public Response deletePermission(String id, UriInfo ui) {
		support.setPermission(context, id, Permission.None);
		return noContent().build();
	}

	@Override
	@CallCounted
	public Response makePermission(PermissionDescription desc, UriInfo ui) {
		support.setPermission(context, desc.userName, desc.permission);
		return created(
				ui.getAbsolutePathBuilder().path("{user}").build(desc.userName))
				.build();
	}
}

/**
 * Description of properties supported by {@link RunSecurityREST}.
 * 
 * @author Donal Fellows
 */
interface SecurityBean extends SupportAware {
	RunSecurityREST connect(TavernaSecurityContext context, TavernaRun run);
}