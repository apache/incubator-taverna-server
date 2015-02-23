/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.mocks;

import static java.util.Calendar.MINUTE;
import static java.util.Collections.unmodifiableList;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.common.Status.Initialized;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.springframework.security.core.context.SecurityContext;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.SecurityContextFactory;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.utils.UsernamePrincipal;

@SuppressWarnings("serial")
public class ExampleRun implements TavernaRun, TavernaSecurityContext {
	String id;
	List<Listener> listeners;
	Workflow workflow;
	Status status;
	Date expiry;
	UsernamePrincipal owner;
	String inputBaclava;
	String outputBaclava;
	java.io.File realRoot;
	List<Input> inputs;
	String name;

	public ExampleRun(UsernamePrincipal creator, Workflow workflow, Date expiry) {
		this.id = randomUUID().toString();
		this.listeners = new ArrayList<>();
		this.status = Initialized;
		this.owner = creator;
		this.workflow = workflow;
		this.expiry = expiry;
		this.inputs = new ArrayList<>();
		listeners.add(new DefaultListener());
	}

	@Override
	public void addListener(Listener l) {
		listeners.add(l);
	}

	@Override
	public void destroy() {
		// This does nothing...
	}

	@Override
	public Date getExpiry() {
		return expiry;
	}

	@Override
	public List<Listener> getListeners() {
		return listeners;
	}

	@Override
	public TavernaSecurityContext getSecurityContext() {
		return this;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public Workflow getWorkflow() {
		return workflow;
	}

	@Override
	public Directory getWorkingDirectory() {
		// LATER: Implement this!
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void setExpiry(Date d) {
		if (d.after(new Date()))
			this.expiry = d;
	}

	@Override
	public String setStatus(Status s) {
		this.status = s;
		return null;
	}

	@Override
	public UsernamePrincipal getOwner() {
		return owner;
	}

	public static class Builder implements RunFactory {
		private int lifetime;

		public Builder(int initialLifetimeMinutes) {
			this.lifetime = initialLifetimeMinutes;
		}

		@Override
		public TavernaRun create(UsernamePrincipal creator, Workflow workflow) {
			Calendar c = GregorianCalendar.getInstance();
			c.add(MINUTE, lifetime);
			return new ExampleRun(creator, workflow, c.getTime());
		}

		@Override
		public boolean isAllowingRunsToStart() {
			return true;
		}
	}

	static final String[] emptyArray = new String[0];

	class DefaultListener implements Listener {
		@Override
		public String getConfiguration() {
			return "";
		}

		@Override
		public String getName() {
			return "default";
		}

		@Override
		public String getType() {
			return "default";
		}

		@Override
		public String[] listProperties() {
			return emptyArray;
		}

		@Override
		public String getProperty(String propName) throws NoListenerException {
			throw new NoListenerException("no such property");
		}

		@Override
		public void setProperty(String propName, String value)
				throws NoListenerException {
			throw new NoListenerException("no such property");
		}
	}

	@Override
	public String getInputBaclavaFile() {
		return inputBaclava;
	}

	@Override
	public List<Input> getInputs() {
		return unmodifiableList(inputs);
	}

	@Override
	public String getOutputBaclavaFile() {
		return outputBaclava;
	}

	class ExampleInput implements Input {
		public String name;
		public String file;
		public String value;
		public String delim;

		public ExampleInput(String name) {
			this.name = name;
		}

		@Override
		public String getFile() {
			return file;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public void setFile(String file) throws FilesystemAccessException,
				BadStateChangeException {
			if (status != Status.Initialized)
				throw new BadStateChangeException();
			checkBadFilename(file);
			this.file = file;
			this.value = null;
			inputBaclava = null;
		}

		@Override
		public void setValue(String value) throws BadStateChangeException {
			if (status != Status.Initialized)
				throw new BadStateChangeException();
			this.value = value;
			this.file = null;
			inputBaclava = null;
		}

		void reset() {
			this.file = null;
			this.value = null;
		}

		@Override
		public String getDelimiter() {
			return delim;
		}

		@Override
		public void setDelimiter(String delimiter)
				throws BadStateChangeException {
			if (status != Status.Initialized)
				throw new BadStateChangeException();
			if (delimiter == null)
				delim = null;
			else
				delim = delimiter.substring(0, 1);
		}
	}

	@Override
	public Input makeInput(String name) throws BadStateChangeException {
		if (status != Status.Initialized)
			throw new BadStateChangeException();
		Input i = new ExampleInput(name);
		inputs.add(i);
		return i;
	}

	static void checkBadFilename(String filename)
			throws FilesystemAccessException {
		if (filename.startsWith("/"))
			throw new FilesystemAccessException("filename may not be absolute");
		if (Arrays.asList(filename.split("/")).contains(".."))
			throw new FilesystemAccessException(
					"filename may not refer to parent");
	}

	@Override
	public void setInputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException {
		if (status != Status.Initialized)
			throw new BadStateChangeException();
		checkBadFilename(filename);
		inputBaclava = filename;
		for (Input i : inputs)
			((ExampleInput) i).reset();
	}

	@Override
	public void setOutputBaclavaFile(String filename)
			throws FilesystemAccessException, BadStateChangeException {
		if (status != Status.Initialized)
			throw new BadStateChangeException();
		if (filename != null)
			checkBadFilename(filename);
		outputBaclava = filename;
	}

	private Date created = new Date();
	@Override
	public Date getCreationTimestamp() {
		return created;
	}

	@Override
	public Date getFinishTimestamp() {
		return null;
	}

	@Override
	public Date getStartTimestamp() {
		return null;
	}

	@Override
	public Credential[] getCredentials() {
		return new Credential[0];
	}

	@Override
	public void addCredential(Credential toAdd) {
	}

	@Override
	public void deleteCredential(Credential toDelete) {
	}

	@Override
	public Trust[] getTrusted() {
		return new Trust[0];
	}

	@Override
	public void addTrusted(Trust toAdd) {
	}

	@Override
	public void deleteTrusted(Trust toDelete) {
	}

	@Override
	public void validateCredential(Credential c)
			throws InvalidCredentialException {
	}

	@Override
	public void validateTrusted(Trust t) throws InvalidCredentialException {
	}

	@Override
	public void initializeSecurityFromSOAPContext(MessageContext context) {
		// Do nothing
	}

	@Override
	public void initializeSecurityFromRESTContext(HttpHeaders headers) {
		// Do nothing
	}

	@Override
	public void conveySecurity() throws GeneralSecurityException, IOException {
		// Do nothing
	}

	@Override
	public SecurityContextFactory getFactory() {
		return null;
	}

	private Set<String> destroyers = new HashSet<String>();
	private Set<String> updaters = new HashSet<String>();
	private Set<String> readers = new HashSet<String>();
	@Override
	public Set<String> getPermittedDestroyers() {
		return destroyers;
	}

	@Override
	public void setPermittedDestroyers(Set<String> destroyers) {
		this.destroyers = destroyers;
		updaters.addAll(destroyers);
		readers.addAll(destroyers);
	}

	@Override
	public Set<String> getPermittedUpdaters() {
		return updaters;
	}

	@Override
	public void setPermittedUpdaters(Set<String> updaters) {
		this.updaters = updaters;
		this.updaters.addAll(destroyers);
		readers.addAll(updaters);
	}

	@Override
	public Set<String> getPermittedReaders() {
		return readers;
	}

	@Override
	public void setPermittedReaders(Set<String> readers) {
		this.readers = readers;
		this.readers.addAll(destroyers);
		this.readers.addAll(updaters);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void initializeSecurityFromContext(SecurityContext securityContext)
			throws Exception {
		// Do nothing
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = (name.length() > 5 ? name.substring(0, 5) : name);
	}

	@Override
	public void ping() throws UnknownRunException {
		// Do nothing
	}

	@Override
	public boolean getGenerateProvenance() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setGenerateProvenance(boolean generateProvenance) {
		// TODO Auto-generated method stub
		
	}
}
