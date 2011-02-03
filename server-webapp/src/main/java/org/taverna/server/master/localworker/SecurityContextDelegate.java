/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.UUID.randomUUID;
import static javax.security.auth.x500.X500Principal.RFC2253;
import static org.taverna.server.master.localworker.AbstractRemoteRunFactory.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.ws.handler.MessageContext;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.localworker.remote.RemoteSecurityContext;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.InvalidCredentialException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaSecurityContext;
import org.taverna.server.master.utils.FilenameUtils;

/**
 * Implementation of a security context.
 * 
 * @author Donal Fellows
 */
public class SecurityContextDelegate implements TavernaSecurityContext {
	private final Principal owner;
	private final List<Credential> credentials = new ArrayList<Credential>();
	private final List<Trust> trusted = new ArrayList<Trust>();
	private final RemoteRunDelegate run;
	private final Object lock = new Object();
	private final Factory factory;

	protected SecurityContextDelegate(RemoteRunDelegate run, Principal owner,
			Factory factory) {
		this.run = run;
		this.owner = owner;
		this.factory = factory;
	}

	/**
	 * Singleton factory. Really is a singleton (and is also very trivial); the
	 * singleton-ness is just about limiting the number of instances of this
	 * around even when lots of serialization is going on.
	 * 
	 * @see Serializable
	 * @author Donal Fellows
	 */
	public static class Factory implements SecurityContextFactory {
		private static Factory instance;
		transient RunDatabase db;
		transient FilenameUtils fileUtils;

		public Factory() {
			if (instance == null)
				instance = this;
		}

		@Required
		public void setRunDatabase(RunDatabase db) {
			this.db = db;
		}

		@Required
		public void setFilenameConverter(FilenameUtils fileUtils) {
			this.fileUtils = fileUtils;
		}

		@Override
		public SecurityContextDelegate create(RemoteRunDelegate run,
				Principal owner) throws Exception {
			return new SecurityContextDelegate(run, owner, this);
		}

		private Object readResolve() {
			return (instance != null) ? instance : (instance = this);
		}
	}

	@Override
	public SecurityContextFactory getFactory() {
		return factory;
	}

	@Override
	public Principal getOwner() {
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
		}
	}

	@Override
	public void deleteCredential(Credential toDelete) {
		synchronized (lock) {
			credentials.remove(toDelete);
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
		}
	}

	@Override
	public void deleteTrusted(Trust toDelete) {
		synchronized (lock) {
			trusted.remove(toDelete);
		}
	}

	/** The type of certificates that are processed if we don't say otherwise. */
	private static final String DEFAULT_CERTIFICATE_TYPE = "X.509";

	@Override
	public void validateCredential(Credential c)
			throws InvalidCredentialException {
		if (c instanceof Credential.CaGridProxy)
			validateProxyCredential((Credential.CaGridProxy) c);
		else if (c instanceof Credential.Password)
			validatePasswordCredential((Credential.Password) c);
		else if (c instanceof Credential.KeyPair)
			validateKeyCredential((Credential.KeyPair) c);
		else
			throw new InvalidCredentialException("unknown credential type");
	}

	private static final char USERNAME_PASSWORD_SEPARATOR = '\u0000';
	private static final String USERNAME_PASSWORD_KEY_ALGORITHM = "DUMMY";
	/** What passwords are encoded as. */
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private void validatePasswordCredential(Credential.Password c) {
		String keyToSave = c.username + USERNAME_PASSWORD_SEPARATOR
				+ c.password;
		c.loadedKey = new SecretKeySpec(keyToSave.getBytes(UTF8),
				USERNAME_PASSWORD_KEY_ALGORITHM);
		c.loadedTrustChain = null;
	}

	private void validateKeyCredential(Credential.KeyPair c)
			throws InvalidCredentialException {
		if (c.credentialName == null || c.credentialName.trim().length() == 0)
			throw new InvalidCredentialException(
					"absent or empty credentialName");

		if (c.credentialFile == null || c.credentialFile.trim().length() == 0)
			throw new InvalidCredentialException(
					"absent or empty credentialFile");
		if (c.fileType == null || c.fileType.trim().length() == 0)
			c.fileType = KeyStore.getDefaultType();
		c.fileType = c.fileType.trim();
		try {
			KeyStore ks = KeyStore.getInstance(c.fileType);
			char[] password = c.unlockPassword.toCharArray();
			ks.load(contents(c.credentialFile), password);
			try {
				c.loadedKey = ks.getKey(c.credentialName, password);
			} catch (UnrecoverableKeyException ignored) {
				c.loadedKey = ks.getKey(c.credentialName, new char[0]);
			}
			if (c.loadedKey == null)
				throw new InvalidCredentialException(
						"no such credential in key store");
			c.loadedTrustChain = ks.getCertificateChain(c.credentialName);
		} catch (InvalidCredentialException e) {
			throw e;
		} catch (Exception e) {
			throw new InvalidCredentialException(e);
		}
	}

	private void validateProxyCredential(Credential.CaGridProxy c)
			throws InvalidCredentialException {
		// Proxies are just normal credentials at this point
		validateKeyCredential(c);

		if (c.authenticationService.toString().length() == 0)
			throw new InvalidCredentialException(
					"missing authenticationService");
		if (c.dorianService.toString().length() == 0)
			throw new InvalidCredentialException("missing dorianService");
	}

	@Override
	public void validateTrusted(Trust t) throws InvalidCredentialException {
		if (t.certificateFile == null || t.certificateFile.trim().length() == 0)
			throw new InvalidCredentialException(
					"absent or empty certificateFile");
		if (t.fileType == null || t.fileType.trim().length() == 0)
			t.fileType = DEFAULT_CERTIFICATE_TYPE;
		t.fileType = t.fileType.trim();
		try {
			t.loadedCertificates = CertificateFactory.getInstance(t.fileType)
					.generateCertificates(contents(t.certificateFile));
		} catch (CertificateException e) {
			throw new InvalidCredentialException(e);
		}
	}

	@Override
	public void initializeSecurityFromContext(ServletContext servletContext) throws Exception {
		// do nothing in this implementation
	}

	@Override
	public void initializeSecurityFromSOAPContext(MessageContext context) {
		// do nothing in this implementation
	}

	@Override
	public void initializeSecurityFromRESTContext(HttpHeaders context) {
		// do nothing in this implementation
	}

	protected KeyStore getInitialKeyStore() throws GeneralSecurityException {
		return KeyStore.getInstance("JCEKS");
	}

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
	public void conveySecurity() throws GeneralSecurityException, IOException {
		if (credentials.isEmpty() && trusted.isEmpty())
			return;
		char[] password = null;
		try {
			password = generateNewPassword();

			log.info("constructing merged keystore");
			KeyStore truststore = getInitialTrustStore();
			KeyStore keystore = getInitialKeyStore();
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
						if (c instanceof Credential.Password)
							addUserPassToKeystore((Credential.Password) c);
						else if (c instanceof Credential.CaGridProxy)
							addPoxyToKeystore((Credential.CaGridProxy) c);
						else
							addKeypairToKeystore((Credential.KeyPair) c);
						keyCount++;
					}
				} finally {
					this.password = null;
					this.uriToAliasMap = null;
					this.keystore = null;
				}
			}

			byte[] trustbytes = null, keybytes = null;
			try {
				trustbytes = serialize(truststore, password);
				keybytes = serialize(keystore, password);

				// Now we've built the security information, ship it off...

				RemoteSecurityContext rc = run.run.getSecurityContext();

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

	protected char[] generateNewPassword() {
		return randomUUID().toString().toCharArray();
	}

	protected void addCertificateToTruststore(KeyStore ts, Certificate cert)
			throws KeyStoreException {
		X509Certificate c = (X509Certificate) cert;
		String owner = X500Utils.getName(c.getSubjectX500Principal(), "CN",
				"COMMONNAME", "OU", "ORGANIZATIONALUNITNAME", "O",
				"ORGANIZATIONNAME");
		String issuer = X500Utils.getName(c.getIssuerX500Principal(), "CN",
				"COMMONNAME", "OU", "ORGANIZATIONALUNITNAME", "O",
				"ORGANIZATIONNAME");
		String alias = "trustedcert#" + owner + "#" + issuer + "#"
				+ X500Utils.getSerial(c);
		ts.setCertificateEntry(alias, c);
	}

	private transient KeyStore keystore;
	private transient char[] password;
	private transient HashMap<URI, String> uriToAliasMap;

	protected final void addKeypairToKeystore(String alias, Credential c)
			throws KeyStoreException {
		if (uriToAliasMap.containsKey(c.serviceURI))
			log.warn("duplicate URI in alias mapping: " + c.serviceURI);
		keystore.setKeyEntry(alias, c.loadedKey, password, c.loadedTrustChain);
		uriToAliasMap.put(c.serviceURI, alias);
	}

	protected void addKeypairToKeystore(Credential.KeyPair c)
			throws KeyStoreException {
		X509Certificate subjectCert = ((X509Certificate) c.loadedTrustChain[0]);
		X500Principal subject = subjectCert.getSubjectX500Principal();
		X500Principal issuer = subjectCert.getIssuerX500Principal();
		String alias = "keypair#"
				+ X500Utils.getName(subject, "CN", "COMMONNAME") + "#"
				+ X500Utils.getName(issuer, "CN", "COMMONNAME") + "#"
				+ X500Utils.getSerial(subjectCert);
		addKeypairToKeystore(alias, c);
	}

	protected void addUserPassToKeystore(Credential.Password c)
			throws KeyStoreException {
		String alias = "password#" + c.serviceURI;
		addKeypairToKeystore(alias, c);
	}

	private void addPoxyToKeystore(Credential.CaGridProxy c)
			throws KeyStoreException {
		String alias = "cagridproxy#" + c.authenticationService + " "
				+ c.dorianService;
		addKeypairToKeystore(alias, c);
	}

	private InputStream contents(String name) throws InvalidCredentialException {
		try {
			File f = (File) factory.fileUtils.getDirEntry(run, name);
			return new ByteArrayInputStream(f.getContents(0, (int) f.getSize()));
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
		run.setDestroyers(destroyers, factory.db);
	}

	@Override
	public Set<String> getPermittedUpdaters() {
		return run.getWriters();
	}

	@Override
	public void setPermittedUpdaters(Set<String> updaters) {
		run.setWriters(updaters, factory.db);
	}

	@Override
	public Set<String> getPermittedReaders() {
		return run.getReaders();
	}

	@Override
	public void setPermittedReaders(Set<String> readers) {
		run.setReaders(readers, factory.db);
	}
}

/**
 * Support class that factors out some of the messier parts of working with
 * X.500 identities and X.509 certificates.
 * 
 * @author Donal Fellows
 */
class X500Utils {
	private static final char DN_SEPARATOR = ',';
	private static final char DN_ESCAPE = '\\';
	private static final char DN_QUOTE = '"';

	/**
	 * Parse the DN from the Principal and extract the CN field.
	 * 
	 * @param id
	 *            The identity to extract the distinguished name from.
	 * @param fields
	 *            The names to look at when finding the field to return. Each
	 *            should be an upper-cased string.
	 * @return The common-name part of the distinguished name, or the literal
	 *         string "<tt>none</tt>" if there is no CN.
	 */
	public static String getName(X500Principal id, String... fields) {
		String dn = id.getName(RFC2253);

		int i = 0;
		int startIndex = 0;
		boolean ignoreThisChar = false;
		boolean inQuotes = false;
		HashMap<String, String> tokenized = new HashMap<String, String>();

		for (i = 0; i < dn.length(); i++)
			if (ignoreThisChar)
				ignoreThisChar = false;
			else if (dn.charAt(i) == DN_QUOTE)
				inQuotes = !inQuotes;
			else if (inQuotes)
				continue;
			else if (dn.charAt(i) == DN_ESCAPE)
				ignoreThisChar = true;
			else if ((dn.charAt(i) == DN_SEPARATOR) && !ignoreThisChar) {
				storeDNField(tokenized, dn.substring(startIndex, i).trim()
						.split("=", 2));
				startIndex = i + 1;
			}
		if (inQuotes || ignoreThisChar)
			log.warn("was parsing invalid DN format");
		// Add last token - after the last delimiter
		storeDNField(tokenized, dn.substring(startIndex).trim().split("=", 2));

		for (String field : fields) {
			String value = tokenized.get(field);
			if (value != null)
				return value;
		}
		return "none";
	}

	private static void storeDNField(HashMap<String, String> container,
			String[] split) {
		if (split == null || split.length != 2)
			return;
		String key = split[0].toUpperCase();
		if (container.containsKey(key))
			log.warn("duplicate field in DN: " + key);
		// LATER: Should the field be de-quoted?
		container.put(key, split[1]);
	}

	/**
	 * Get the serial number from a certificate as a hex string.
	 * 
	 * @param cert
	 *            The certificate to extract from.
	 * @return A hex string, in upper-case.
	 */
	public static String getSerial(X509Certificate cert) {
		return new BigInteger(1, cert.getSerialNumber().toByteArray())
				.toString(16).toUpperCase();
	}
}