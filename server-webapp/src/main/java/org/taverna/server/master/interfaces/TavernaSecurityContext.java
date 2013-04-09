/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.interfaces;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.springframework.security.core.context.SecurityContext;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Security context for a workflow run.
 * 
 * @author Donal Fellows
 */
public interface TavernaSecurityContext {
	/**
	 * @return Who owns the security context.
	 */
	UsernamePrincipal getOwner();

	/**
	 * Describe the names of the users (as extracted from their
	 * {@link Principal} objects) that may destroy the run or manipulate its
	 * lifetime.
	 * 
	 * @return The names of the users who may use destroy operations. Read-only.
	 */
	Set<String> getPermittedDestroyers();

	/**
	 * Sets the collection of names of users (as extracted from their
	 * {@link Principal} objects) that may destroy the run or manipulate its
	 * lifetime.
	 * 
	 * @param destroyers
	 *            The names of the users who may use destroy operations.
	 */
	void setPermittedDestroyers(Set<String> destroyers);

	/**
	 * Describe the names of the users (as extracted from their
	 * {@link Principal} objects) that may update the run (including writing to
	 * files).
	 * 
	 * @return The names of the users who may use update operations. Read-only.
	 */
	Set<String> getPermittedUpdaters();

	/**
	 * Sets the collection of names of users (as extracted from their
	 * {@link Principal} objects) that may update the run (including writing to
	 * its files).
	 * 
	 * @param updaters
	 *            The names of the users who may use update operations.
	 */
	void setPermittedUpdaters(Set<String> updaters);

	/**
	 * Describe the names of the users (as extracted from their
	 * {@link Principal} objects) that may read from the run (including its
	 * files).
	 * 
	 * @return The names of the users who may use read operations. Read-only.
	 */
	Set<String> getPermittedReaders();

	/**
	 * Sets the collection of names of users (as extracted from their
	 * {@link Principal} objects) that may read from the run (including its
	 * files).
	 * 
	 * @param readers
	 *            The names of the users who may use read operations.
	 */
	void setPermittedReaders(Set<String> readers);

	/**
	 * @return The credentials owned by the user. Never <tt>null</tt>.
	 */
	Credential[] getCredentials();

	/**
	 * Add a credential to the owned set or replaces the old version with the
	 * new one.
	 * 
	 * @param toAdd
	 *            The credential to add.
	 */
	void addCredential(Credential toAdd);

	/**
	 * Remove a credential from the owned set. It's not a failure to remove
	 * something that isn't in the set.
	 * 
	 * @param toDelete
	 *            The credential to remove.
	 */
	void deleteCredential(Credential toDelete);

	/**
	 * Tests if the credential is valid. This includes testing whether the
	 * underlying credential file exists and can be unlocked by the password in
	 * the {@link Credential} object.
	 * 
	 * @param c
	 *            The credential object to validate.
	 * @throws InvalidCredentialException
	 *             If it is invalid.
	 */
	void validateCredential(Credential c) throws InvalidCredentialException;

	/**
	 * @return The identities trusted by the user. Never <tt>null</tt>.
	 */
	Trust[] getTrusted();

	/**
	 * Add an identity to the trusted set.
	 * 
	 * @param toAdd
	 *            The identity to add.
	 */
	void addTrusted(Trust toAdd);

	/**
	 * Remove an identity from the trusted set. It's not a failure to remove
	 * something that isn't in the set.
	 * 
	 * @param toDelete
	 *            The identity to remove.
	 */
	void deleteTrusted(Trust toDelete);

	/**
	 * Tests if the trusted identity descriptor is valid. This includes checking
	 * whether the underlying trusted identity file exists.
	 * 
	 * @param t
	 *            The trusted identity descriptor to check.
	 * @throws InvalidCredentialException
	 *             If it is invalid.
	 */
	void validateTrusted(Trust t) throws InvalidCredentialException;

	/**
	 * Establish the security context from how the owning workflow run was
	 * created. In particular, this gives an opportunity for boot-strapping
	 * things with any delegateable credentials.
	 * 
	 * @param securityContext
	 *            The security context associated with the request that caused
	 *            the workflow to be created.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	void initializeSecurityFromContext(SecurityContext securityContext)
			throws Exception;

	/**
	 * Establish the security context from how the owning workflow run was
	 * created. In particular, this gives an opportunity for boot-strapping
	 * things with any delegateable credentials.
	 * 
	 * @param context
	 *            The full information about the request that caused the
	 *            workflow to be created.
	 */
	void initializeSecurityFromSOAPContext(MessageContext context);

	/**
	 * Establish the security context from how the owning workflow run was
	 * created. In particular, this gives an opportunity for boot-strapping
	 * things with any delegateable credentials.
	 * 
	 * @param headers
	 *            The full information about the request that caused the
	 *            workflow to be created.
	 */
	void initializeSecurityFromRESTContext(HttpHeaders headers);

	/**
	 * Transfer the security context to the remote system.
	 * 
	 * @throws IOException
	 *             If the communication fails.
	 * @throws GeneralSecurityException
	 *             If the assembly of the context fails.
	 * @throws ImplementationException
	 *             If the local worker has problems with creating the realized
	 *             security context.
	 */
	void conveySecurity() throws GeneralSecurityException, IOException,
			ImplementationException;

	/**
	 * @return The factory that created this security context.
	 */
	SecurityContextFactory getFactory();
}
