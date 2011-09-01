/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.UUID.randomUUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.JDKKeyStore.BouncyCastleStore;
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
	private Log log = LogFactory.getLog("Taverna.Server.LocalWorker");
	private final UsernamePrincipal owner;
	private final List<Credential> credentials = new ArrayList<Credential>();
	private final List<Trust> trusted = new ArrayList<Trust>();
	private final RemoteRunDelegate run;
	private final Object lock = new Object();
	private final SecurityContextFactory factory;

	private transient KeyStoreSpi keystore;
	private transient char[] password;
	private transient HashMap<URI, String> uriToAliasMap;

	/** The type of certificates that are processed if we don't say otherwise. */
	private static final String DEFAULT_CERTIFICATE_TYPE = "X.509";

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

	@Override
	public void addCredential(Credential toAdd) {
		synchronized (lock) {
			int idx = credentials.indexOf(toAdd);
			if (idx != -1)
				credentials.set(idx, toAdd);
			else
				credentials.add(toAdd);
			factory.db.flushToDisk(run);
		}
	}

	@Override
	public void deleteCredential(Credential toDelete) {
		synchronized (lock) {
			credentials.remove(toDelete);
			factory.db.flushToDisk(run);
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
			factory.db.flushToDisk(run);
		}
	}

	@Override
	public void deleteTrusted(Trust toDelete) {
		synchronized (lock) {
			trusted.remove(toDelete);
			factory.db.flushToDisk(run);
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
				|| t.certificateFile.trim().length() == 0)
			throw new InvalidCredentialException(
					"absent or empty certificateFile");
		else {
			contentsAsStream = contents(t.certificateFile);
			t.certificateBytes = null;
		}
		if (t.fileType == null || t.fileType.trim().length() == 0)
			t.fileType = DEFAULT_CERTIFICATE_TYPE;
		t.fileType = t.fileType.trim();
		try {
			t.loadedCertificates = CertificateFactory.getInstance(t.fileType)
					.generateCertificates(contentsAsStream);
		} catch (CertificateException e) {
			throw new InvalidCredentialException(e);
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
	 * Get an empty keystore for use with credentials (client certs, passwords,
	 * etc.)
	 * 
	 * @return A keystore <i>back-end</i>, so we circumvent some of the more
	 *         annoying features of the API.
	 * @throws GeneralSecurityException
	 */
	protected KeyStoreSpi getInitialKeyStore() throws GeneralSecurityException {
		return new BouncyCastleStore();
	}

	/**
	 * Get an empty trust-store for use with trusted certificates.
	 * 
	 * @return A trust-store
	 * @throws GeneralSecurityException
	 */
	protected KeyStore getInitialTrustStore() throws GeneralSecurityException {
		return KeyStore.getInstance("JCEKS");
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
			KeyStore truststore = getInitialTrustStore();
			KeyStoreSpi keystore = getInitialKeyStore();
			HashMap<URI, String> uriToAliasMap = new HashMap<URI, String>();
			int trustedCount = 0, keyCount = 0;

			synchronized (lock) {
				try {
					for (Trust t : trusted)
						for (Certificate cert : t.loadedCertificates) {
							addCertificateToTruststore(truststore, cert);
							trustedCount++;
						}

					this.password = password;
					this.uriToAliasMap = uriToAliasMap;
					this.keystore = keystore;
					for (Credential c : credentials) {
						addCredentialToKeystore(c);
						keyCount++;
					}
				} finally {
					this.password = null;
					this.uriToAliasMap = null;
					this.keystore = null;
					credentials.clear();
					trusted.clear();
					factory.db.flushToDisk(run);
				}
			}

			byte[] trustbytes = null, keybytes = null;
			try {
				trustbytes = serialize(truststore, password);
				keybytes = serialize(keystore, password);

				// Now we've built the security information, ship it off...

				log.info("transfering merged truststore with " + trustedCount
						+ " entries");
				rc.setTruststore(trustbytes);

				log.info("transfering merged keystore with " + keyCount
						+ " entries");
				rc.setKeystore(keybytes);
				rc.setPassword(password);

				log.info("transfering serviceURL->alias map with "
						+ uriToAliasMap.size() + " entries");
				rc.setUriToAliasMap(uriToAliasMap);
			} finally {
				blankOut(trustbytes);
				blankOut(keybytes);
			}
		} finally {
			blankOut(password);
		}
	}

	private static void blankOut(char[] ary) {
		if (ary == null)
			return;
		for (int i = 0; i < ary.length; i++)
			ary[i] = ' ';
	}

	private static void blankOut(byte[] ary) {
		if (ary == null)
			return;
		for (int i = 0; i < ary.length; i++)
			ary[i] = 0;
	}

	private static byte[] serialize(KeyStore ks, char[] password)
			throws GeneralSecurityException, IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ks.store(stream, password);
		stream.close();
		return stream.toByteArray();
	}

	private static byte[] serialize(KeyStoreSpi ks, char[] password)
			throws GeneralSecurityException, IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ks.engineStore(stream, password);
		stream.close();
		return stream.toByteArray();
	}

	/**
	 * @return A new password with a reasonable level of randomness.
	 */
	protected char[] generateNewPassword() {
		return randomUUID().toString().toCharArray();
	}

	/**
	 * Adds a service certificate to the set that are trusted.
	 * 
	 * @param ts
	 *            The trust-store.
	 * @param cert
	 *            The certificate to add
	 * @throws KeyStoreException
	 */
	protected void addCertificateToTruststore(KeyStore ts, Certificate cert)
			throws KeyStoreException {
		X509Certificate c = (X509Certificate) cert;
		String owner = factory.x500Utils.getName(c.getSubjectX500Principal(),
				"CN", "COMMONNAME", "OU", "ORGANIZATIONALUNITNAME", "O",
				"ORGANIZATIONNAME");
		String issuer = factory.x500Utils.getName(c.getIssuerX500Principal(),
				"CN", "COMMONNAME", "OU", "ORGANIZATIONALUNITNAME", "O",
				"ORGANIZATIONNAME");
		String alias = "trustedcert#" + owner + "#" + issuer + "#"
				+ factory.x500Utils.getSerial(c);
		ts.setCertificateEntry(alias, c);
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
		keystore.engineSetKeyEntry(alias, c.loadedKey, password,
				c.loadedTrustChain);
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
	 * Read a file up to 20kB in size.
	 * 
	 * @param name
	 *            The path name of the file, relative to the context run's
	 *            working directory.
	 * @return A stream of the file's contents.
	 * @throws InvalidCredentialException
	 *             If anything goes wrong.
	 */
	InputStream contents(String name) throws InvalidCredentialException {
		try {
			File f = (File) factory.fileUtils.getDirEntry(run, name);
			long size = f.getSize();
			if (size > 20 * 1024)
				throw new InvalidCredentialException("20kB limit hit");
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
}