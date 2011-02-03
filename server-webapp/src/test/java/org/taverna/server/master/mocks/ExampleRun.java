/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.mocks;

import static java.util.Calendar.MINUTE;
import static java.util.Collections.unmodifiableList;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.common.Status.Initialized;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.factories.RunFactory;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.localworker.SecurityContextFactory;

public class ExampleRun implements TavernaRun, TavernaSecurityContext {
	String id;
	List<Listener> listeners;
	Workflow workflow;
	Status status;
	Date expiry;
	Principal owner;
	String inputBaclava;
	String outputBaclava;
	java.io.File realRoot;
	List<Input> inputs;

	public ExampleRun(Principal creator, Workflow workflow, Date expiry) {
		this.id = randomUUID().toString();
		this.listeners = new ArrayList<Listener>();
		this.status = Initialized;
		this.owner = creator;
		this.workflow = workflow;
		this.expiry = expiry;
		this.inputs = new ArrayList<Input>();
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
	public void setStatus(Status s) {
		this.status = s;
	}

	@Override
	public Principal getOwner() {
		return owner;
	}

	public static class Builder implements RunFactory {
		private int lifetime;

		public Builder(int initialLifetimeMinutes) {
			this.lifetime = initialLifetimeMinutes;
		}

		@Override
		public TavernaRun create(Principal creator, Workflow workflow) {
			Calendar c = GregorianCalendar.getInstance();
			c.add(MINUTE, lifetime);
			return new ExampleRun(creator, workflow, c.getTime());
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

	@Override
	public Date getCreationTimestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getFinishTimestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getStartTimestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credential[] getCredentials() {
		// TODO Auto-generated method stub
		return new Credential[0];
	}

	@Override
	public void addCredential(Credential toAdd) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteCredential(Credential toDelete) {
		// TODO Auto-generated method stub

	}

	@Override
	public Trust[] getTrusted() {
		// TODO Auto-generated method stub
		return new Trust[0];
	}

	@Override
	public void addTrusted(Trust toAdd) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteTrusted(Trust toDelete) {
		// TODO Auto-generated method stub

	}

	@Override
	public void validateCredential(Credential c)
			throws InvalidCredentialException {
		// TODO Auto-generated method stub

	}

	@Override
	public void validateTrusted(Trust t) throws InvalidCredentialException {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeSecurityFromSOAPContext(MessageContext context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initializeSecurityFromRESTContext(HttpHeaders headers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void conveySecurity() throws GeneralSecurityException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public SecurityContextFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getPermittedDestroyers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPermittedDestroyers(Set<String> destroyers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getPermittedUpdaters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPermittedUpdaters(Set<String> updaters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getPermittedReaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPermittedReaders(Set<String> readers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public void initializeSecurityFromContext(ServletContext servletContext)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
}
