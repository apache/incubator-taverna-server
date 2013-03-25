/*
 * Copyright (C) 2010-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.lang.String.format;
import static java.util.Arrays.fill;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.defaults.Default.CERTIFICATE_FIELD_NAMES;
import static org.taverna.server.master.defaults.Default.CERTIFICATE_TYPE;
import static org.taverna.server.master.defaults.Default.CREDENTIAL_FILE_SIZE_LIMIT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.taverna.server.localworker.remote.ImplementationException;
import org.taverna.server.localworker.remote.RemoteSecurityContext;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.utils.UsernamePrincipal;

/**
 * Implementation of a security context.
 * 
 * @author Donal Fellows
 */
public abstract class SecurityContextDelegate implements TavernaSecurityContext {
	Log log = LogFactory.getLog("Taverna.Server.LocalWorker");
	private final UsernamePrincipal owner;
	private final List<Credential> credentials = new ArrayList<Credential>();
	private final List<Trust> trusted = new ArrayList<Trust>();
	private final RemoteRunDelegate run;
	private final Object lock = new Object();
	final SecurityContextFactory factory;

	private transient Keystore keystore;
	private transient HashMap<URI, String> uriToAliasMap;

	/**
	 * Initialise the context delegate.
	 * 
	 * @param run
	 *            What workflow run is this for?
	 * @param owner
	 *            Who owns the workflow run?
	 * @param factory
	 *            What class built this object?
	 */
	protected SecurityContextDelegate(RemoteRunDelegate run,
			UsernamePrincipal owner, SecurityContextFactory factory) {
		this.run = run;
		this.owner = owner;
		this.factory = factory;
	}

	@Override
	public SecurityContextFactory getFactory() {
		return factory;
	}

	@Override
	public UsernamePrincipal getOwner() {
		return owner;
	}

	@Override
	public Credential[] getCredentials() {
		synchronized (lock) {
			return credentials.toArray(new Credential[credentials.size()]);
		}
	}

	/**
	 * Get the human-readable name of a principal.
	 * 
	 * @param principal
	 *            The principal being decoded.
	 * @return A name.
	 */
	protected final String getPrincipalName(X500Principal principal) {
		return factory.x500Utils.getName(principal, CERTIFICATE_FIELD_NAMES);
	}

	/**
	 * Cause the current state to be flushed to the database.
	 */
	protected final void flushToDB() {
		factory.db.flushToDisk(run);
	}

	@Override
	public void addCredential(Credential toAdd) {
		synchronized (lock) {
			int idx = credentials.indexOf(toAdd);
			if (idx != -1)
				credentials.set(idx, toAdd);
			else
				credentials.add(toAdd);
			flushToDB();
		}
	}

	@Override
	public void deleteCredential(Credential toDelete) {
		synchronized (lock) {
			credentials.remove(toDelete);
			flushToDB();
		}
	}

	@Override
	public Trust[] getTrusted() {
		synchronized (lock) {
			return trusted.toArray(new Trust[trusted.size()]);
		}
	}

	@Override
	public void addTrusted(Trust toAdd) {
		synchronized (lock) {
			int idx = trusted.indexOf(toAdd);
			if (idx != -1)
				trusted.set(idx, toAdd);
			else
				trusted.add(toAdd);
			flushToDB();
		}
	}

	@Override
	public void deleteTrusted(Trust toDelete) {
		synchronized (lock) {
			trusted.remove(toDelete);
			flushToDB();
		}
	}

	@Override
	public abstract void validateCredential(Credential c)
			throws InvalidCredentialException;

	@Override
	public void validateTrusted(Trust t) throws InvalidCredentialException {
		InputStream contentsAsStream;
		if (t.certificateBytes != null && t.certificateBytes.length > 0) {
			contentsAsStream = new ByteArrayInputStream(t.certificateBytes);
			t.certificateFile = null;
		} else if (t.certificateFile == null
				|| t.certificateFile.trim().isEmpty())
			throw new InvalidCredentialException(
					"absent or empty certificateFile");
		else {
			contentsAsStream = contents(t.certificateFile);
			t.certificateBytes = null;
		}
		t.serverName = null;
		if (t.fileType == null || t.fileType.trim().isEmpty())
			t.fileType = CERTIFICATE_TYPE;
		t.fileType = t.fileType.trim();
		try {
			t.loadedCertificates = CertificateFactory.getInstance(t.fileType)
					.generateCertificates(contentsAsStream);
			t.serverName = new ArrayList<String>(t.loadedCertificates.size());
			for (Certificate c : t.loadedCertificates)
				t.serverName.add(getPrincipalName(((X509Certificate) c)
						.getSubjectX500Principal()));
		} catch (CertificateException e) {
			throw new InvalidCredentialException(e);
		} catch (ClassCastException e) {
			// Do nothing; truncates the list of server names
		}
	}

	@Override
	public void initializeSecurityFromContext(SecurityContext securityContext)
			throws Exception {
		// This is how to get the info from Spring Security
		Authentication auth = securityContext.getAuthentication();
		if (auth == null)
			return;
		auth.getPrincipal();
		// do nothing else in this implementation
	}

	@Override
	public void initializeSecurityFromSOAPContext(MessageContext context) {
		// do nothing in this implementation
	}

	@Override
	public void initializeSecurityFromRESTContext(HttpHeaders context) {
		// do nothing in this implementation
	}

	/**
	 * Builds and transfers a keystore with suitable credentials to the back-end
	 * workflow execution engine.
	 * 
	 * @throws GeneralSecurityException
	 *             If the manipulation of the keystore, keys or certificates
	 *             fails.
	 * @throws IOException
	 *             If there are problems building the data (should not happen).
	 * @throws RemoteException
	 *             If the conveyancing fails.
	 */
	@Override
	public final void conveySecurity() throws GeneralSecurityException,
			IOException, ImplementationException {
		RemoteSecurityContext rc = run.run.getSecurityContext();

		synchronized (lock) {
			if (credentials.isEmpty() && trusted.isEmpty())
				return;
		}
		char[] password = null;
		try {
			password = generateNewPassword();

			log.info("constructing merged keystore");
			Truststore truststore = new Truststore(password);
			Keystore keystore = new Keystore(password);
			HashMap<URI, String> uriToAliasMap = new HashMap<URI, String>();
			int trustedCount = 0, keyCount = 0;

			synchronized (lock) {
				try {
					for (Trust t : trusted)
						for (Certificate cert : t.loadedCertificates) {
							truststore.addCertificate(cert);
							trustedCount++;
						}

					this.uriToAliasMap = uriToAliasMap;
					this.keystore = keystore;
					for (Credential c : credentials) {
						addCredentialToKeystore(c);
						keyCount++;
					}
				} finally {
					this.uriToAliasMap = null;
					this.keystore = null;
					credentials.clear();
					trusted.clear();
					flushToDB();
				}
			}

			byte[] trustbytes = null, keybytes = null;
			try {
				trustbytes = truststore.serialize();
				keybytes = keystore.serialize();

				// Now we've built the security information, ship it off...

				log.info("transfering merged truststore with " + trustedCount
						+ " entries");
				rc.setTruststore(trustbytes);

				log.info("transfering merged keystore with " + keyCount
						+ " entries");
				rc.setKeystore(keybytes);
			} finally {
				if (trustbytes != null)
					fill(trustbytes, (byte) 0);
				if (keybytes != null)
					fill(keybytes, (byte) 0);
			}
			rc.setPassword(password);

			log.info("transferring serviceURL->alias map with "
					+ uriToAliasMap.size() + " entries");
			rc.setUriToAliasMap(uriToAliasMap);
		} finally {
			if (password != null)
				fill(password, ' ');
		}

		synchronized (lock) {
			conveyExtraSecuritySettings(rc);
		}
	}

	/**
	 * Hook that allows additional information to be conveyed to the remote run.
	 * 
	 * @param remoteSecurityContext
	 *            The remote resource that information would be passed to.
	 * @throws IOException
	 *             If anything goes wrong with the communication.
	 */
	protected void conveyExtraSecuritySettings(
			RemoteSecurityContext remoteSecurityContext) throws IOException {
		// Does nothing by default; overrideable
	}

	/**
	 * @return A new password with a reasonable level of randomness.
	 */
	protected final char[] generateNewPassword() {
		return randomUUID().toString().toCharArray();
	}

	/**
	 * Adds a credential to the current keystore.
	 * 
	 * @param alias
	 *            The alias to create within the keystore.
	 * @param c
	 *            The key-pair.
	 * @throws KeyStoreException
	 */
	protected final void addKeypairToKeystore(String alias, Credential c)
			throws KeyStoreException {
		if (uriToAliasMap.containsKey(c.serviceURI))
			log.warn("duplicate URI in alias mapping: " + c.serviceURI);
		keystore.addKey(alias, c.loadedKey, c.loadedTrustChain);
		uriToAliasMap.put(c.serviceURI, alias);
	}

	/**
	 * Adds a credential to the current keystore.
	 * 
	 * @param c
	 *            The credential to add.
	 * @throws KeyStoreException
	 */
	public abstract void addCredentialToKeystore(Credential c)
			throws KeyStoreException;

	/**
	 * Read a file up to {@value #FILE_SIZE_LIMIT}kB in size.
	 * 
	 * @param name
	 *            The path name of the file, relative to the context run's
	 *            working directory.
	 * @return A stream of the file's contents.
	 * @throws InvalidCredentialException
	 *             If anything goes wrong.
	 */
	final InputStream contents(String name) throws InvalidCredentialException {
		try {
			File f = (File) factory.fileUtils.getDirEntry(run, name);
			long size = f.getSize();
			if (size > CREDENTIAL_FILE_SIZE_LIMIT * 1024)
				throw new InvalidCredentialException(CREDENTIAL_FILE_SIZE_LIMIT
						+ "kB limit hit");
			return new ByteArrayInputStream(f.getContents(0, (int) size));
		} catch (NoDirectoryEntryException e) {
			throw new InvalidCredentialException(e);
		} catch (FilesystemAccessException e) {
			throw new InvalidCredentialException(e);
		} catch (ClassCastException e) {
			throw new InvalidCredentialException("not a file", e);
		}
	}

	@Override
	public Set<String> getPermittedDestroyers() {
		return run.getDestroyers();
	}

	@Override
	public void setPermittedDestroyers(Set<String> destroyers) {
		run.setDestroyers(destroyers);
	}

	@Override
	public Set<String> getPermittedUpdaters() {
		return run.getWriters();
	}

	@Override
	public void setPermittedUpdaters(Set<String> updaters) {
		run.setWriters(updaters);
	}

	@Override
	public Set<String> getPermittedReaders() {
		return run.getReaders();
	}

	@Override
	public void setPermittedReaders(Set<String> readers) {
		run.setReaders(readers);
	}

	/**
	 * Reinstall the credentials and the trust extracted from serialization to
	 * the database.
	 * 
	 * @param credentials
	 *            The credentials to reinstall.
	 * @param trust
	 *            The trusted certificates to reinstall.
	 */
	void setCredentialsAndTrust(Credential[] credentials, Trust[] trust) {
		synchronized (lock) {
			this.credentials.clear();
			if (credentials != null)
				for (Credential c : credentials)
					try {
						validateCredential(c);
						this.credentials.add(c);
					} catch (InvalidCredentialException e) {
						log.warn("failed to revalidate credential: " + c, e);
					}
			this.trusted.clear();
			if (trust != null)
				for (Trust t : trust)
					try {
						validateTrusted(t);
						this.trusted.add(t);
					} catch (InvalidCredentialException e) {
						log.warn("failed to revalidate trust assertion: " + t,
								e);
					}
		}
	}

	/**
	 * A trust store that can only be added to or serialized. Only trusted
	 * certificates can be placed in it.
	 * 
	 * @author Donal Fellows
	 */
	class Truststore {
		private KeyStore ks;
		private char[] password;

		Truststore(char[] password) throws GeneralSecurityException {
			this.password = password.clone();
			ks = KeyStore.getInstance("UBER", "BC");
			try {
				ks.load(null, this.password);
			} catch (IOException e) {
				throw new GeneralSecurityException(
						"problem initializing blank truststore", e);
			}
		}

		/**
		 * Add a trusted certificate to the truststore. No certificates can be
		 * added after the truststore is serialized.
		 * 
		 * @param cert
		 *            The certificate (typically belonging to a root CA) to add.
		 * @throws KeyStoreException
		 *             If anything goes wrong.
		 */
		public void addCertificate(Certificate cert) throws KeyStoreException {
			if (ks == null)
				throw new IllegalStateException("truststore already written");
			X509Certificate c = (X509Certificate) cert;
			String alias = format("trustedcert#%s#%s#%s",
					getPrincipalName(c.getSubjectX500Principal()),
					getPrincipalName(c.getIssuerX500Principal()),
					factory.x500Utils.getSerial(c));
			ks.setCertificateEntry(alias, c);
			if (factory.logSecurityDetails)
				log.debug("added cert with alias \"" + alias + "\" of type "
						+ c.getClass().getCanonicalName());
		}

		/**
		 * Get the byte serialization of this truststore. This can only be
		 * fetched exactly once.
		 * 
		 * @return The serialization.
		 * @throws GeneralSecurityException
		 *             If anything goes wrong.
		 */
		public byte[] serialize() throws GeneralSecurityException, IOException {
			if (ks == null)
				throw new IllegalStateException("truststore already written");
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try {
				ks.store(stream, password);
				stream.close();
				if (factory.logSecurityDetails)
					log.debug("serialized UBER/BC truststore (size: "
							+ ks.size() + ") with password \""
							+ new String(password) + "\"");
			} catch (IOException e) {
				throw new GeneralSecurityException(
						"problem serializing truststore", e);
			}
			fill(password, ' ');
			ks = null;
			return stream.toByteArray();
		}

		@Override
		protected void finalize() {
			fill(password, ' ');
			ks = null;
		}
	}

	/**
	 * A key store that can only be added to or serialized. Only keys can be
	 * placed in it.
	 * 
	 * @author Donal Fellows
	 */
	class Keystore {
		private KeyStore ks;
		private char[] password;

		Keystore(char[] password) throws GeneralSecurityException {
			this.password = password.clone();

			ks = KeyStore.getInstance("UBER", "BC");
			try {
				ks.load(null, password);
			} catch (IOException e) {
				throw new GeneralSecurityException(
						"problem initializing blank keystore", e);
			}
		}

		/**
		 * Add a key to the keystore. No keys can be added after the keystore is
		 * serialized.
		 * 
		 * @param alias
		 *            The alias of the key.
		 * @param key
		 *            The secret/private key to add.
		 * @param trustChain
		 *            The trusted certificate chain of the key. Should be
		 *            <tt>null</tt> for secret keys.
		 * @throws KeyStoreException
		 *             If anything goes wrong.
		 */
		public void addKey(String alias, Key key, Certificate[] trustChain)
				throws KeyStoreException {
			if (ks == null)
				throw new IllegalStateException("keystore already written");
			ks.setKeyEntry(alias, key, password, trustChain);
			if (factory.logSecurityDetails)
				log.debug("added key with alias \"" + alias + "\" of type "
						+ key.getClass().getCanonicalName());
		}

		/**
		 * Get the byte serialization of this keystore. This can only be fetched
		 * exactly once.
		 * 
		 * @return The serialization.
		 * @throws GeneralSecurityException
		 *             If anything goes wrong.
		 */
		public byte[] serialize() throws GeneralSecurityException {
			if (ks == null)
				throw new IllegalStateException("keystore already written");
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try {
				ks.store(stream, password);
				stream.close();
				if (factory.logSecurityDetails)
					log.debug("serialized UBER/BC keystore (size: " + ks.size()
							+ ") with password \"" + new String(password)
							+ "\"");
			} catch (IOException e) {
				throw new GeneralSecurityException(
						"problem serializing keystore", e);
			}
			fill(password, ' ');
			ks = null;
			return stream.toByteArray();
		}

		@Override
		protected void finalize() {
			fill(password, ' ');
			ks = null;
		}
	}
}