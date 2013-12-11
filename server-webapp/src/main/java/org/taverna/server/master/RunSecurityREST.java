/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.taverna.server.master.common.Status.Initialized;
import static org.taverna.server.master.common.Uri.secure;
import static org.taverna.server.master.utils.RestUtils.opt;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.taverna.server.master.api.SecurityBean;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Permission;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoCredentialException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.rest.TavernaServerSecurityREST;
import org.taverna.server.master.utils.CallTimeLogger.PerfLogged;
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
	@PerfLogged
	public Descriptor describe(UriInfo ui) {
		return new Descriptor(secure(ui).path("{element}"), context.getOwner()
				.getName(), context.getCredentials(), context.getTrusted());
	}

	@Override
	@CallCounted
	@PerfLogged
	public String getOwner() {
		return context.getOwner().getName();
	}

	@Override
	@CallCounted
	@PerfLogged
	public CredentialList listCredentials() {
		return new CredentialList(context.getCredentials());
	}

	@Override
	@CallCounted
	@PerfLogged
	public CredentialHolder getParticularCredential(String id)
			throws NoCredentialException {
		for (Credential c : context.getCredentials())
			if (c.id.equals(id))
				return new CredentialHolder(c);
		throw new NoCredentialException();
	}

	@Override
	@CallCounted
	@PerfLogged
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
	@PerfLogged
	public Response addCredential(CredentialHolder cred, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		Credential c = cred.credential;
		c.id = randomUUID().toString();
		URI uri = secure(ui).path("{id}").build(c.id);
		c.href = uri.toString();
		context.validateCredential(c);
		context.addCredential(c);
		return created(uri).build();
	}

	@Override
	@CallCounted
	@PerfLogged
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
	@PerfLogged
	public Response deleteCredential(String id, UriInfo ui)
			throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		context.deleteCredential(new Credential.Dummy(id));
		return noContent().build();
	}

	@Override
	@CallCounted
	@PerfLogged
	public TrustList listTrusted() {
		return new TrustList(context.getTrusted());
	}

	@Override
	@CallCounted
	@PerfLogged
	public Trust getParticularTrust(String id) throws NoCredentialException {
		for (Trust t : context.getTrusted())
			if (t.id.equals(id))
				return t;
		throw new NoCredentialException();
	}

	@Override
	@CallCounted
	@PerfLogged
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
	@PerfLogged
	public Response addTrust(Trust t, UriInfo ui)
			throws InvalidCredentialException, BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		t.id = randomUUID().toString();
		URI uri = secure(ui).path("{id}").build(t.id);
		t.href = uri.toString();
		context.validateTrusted(t);
		context.addTrusted(t);
		return created(uri).build();
	}

	@Override
	@CallCounted
	@PerfLogged
	public Response deleteAllTrusts(UriInfo ui) throws BadStateChangeException {
		if (run.getStatus() != Initialized)
			throw new BadStateChangeException();
		for (Trust t : context.getTrusted())
			context.deleteTrusted(t);
		return noContent().build();
	}

	@Override
	@CallCounted
	@PerfLogged
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
	@PerfLogged
	public PermissionsDescription describePermissions(UriInfo ui) {
		Map<String, Permission> perm = support.getPermissionMap(context);
		return new PermissionsDescription(secure(ui).path("{id}"), perm);
	}

	@Override
	@CallCounted
	@PerfLogged
	public Permission describePermission(String id) {
		return support.getPermission(context, id);
	}

	@Override
	@CallCounted
	@PerfLogged
	public Permission setPermission(String id, Permission perm) {
		support.setPermission(context, id, perm);
		return support.getPermission(context, id);
	}

	@Override
	@CallCounted
	@PerfLogged
	public Response deletePermission(String id, UriInfo ui) {
		support.setPermission(context, id, Permission.None);
		return noContent().build();
	}

	@Override
	@CallCounted
	@PerfLogged
	public Response makePermission(PermissionDescription desc, UriInfo ui) {
		support.setPermission(context, desc.userName, desc.permission);
		return created(secure(ui).path("{user}").build(desc.userName)).build();
	}

	@Override
	@CallCounted
	public Response descriptionOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response ownerOptions() {
		return opt();
	}

	@Override
	@CallCounted
	public Response credentialsOptions() {
		return opt("POST", "DELETE");
	}

	@Override
	@CallCounted
	public Response credentialOptions(String id) {
		return opt("PUT", "DELETE");
	}

	@Override
	@CallCounted
	public Response trustsOptions() {
		return opt("POST", "DELETE");
	}

	@Override
	@CallCounted
	public Response trustOptions(String id) {
		return opt("PUT", "DELETE");
	}

	@Override
	@CallCounted
	public Response permissionsOptions() {
		return opt("POST");
	}

	@Override
	@CallCounted
	public Response permissionOptions(String id) {
		return opt("PUT", "DELETE");
	}
}