/*
 * Copyright (C) 2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.Holder;

/**
 * Obtains the certificate chain for an arbitrary SSL service. Maintains a
 * cache.
 * 
 * @author Donal Fellows
 */
public class CertificateChainFetcher {
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getKeystoreType() {
		return keystoreType;
	}

	public void setKeystoreType(String keystoreType) {
		this.keystoreType = keystoreType;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	private boolean secure = true;
	private String protocol = "TLS";
	private String keystoreType = KeyStore.getDefaultType();
	private String algorithm = TrustManagerFactory.getDefaultAlgorithm();
	private int timeout = 10000;

	/**
	 * Get the certificate chain for a service.
	 * 
	 * @param host
	 *            The host (name or IP address) to contact the service on.
	 * @param port
	 *            The port to contact the service on.
	 * @return The certificate chain, or <tt>null</tt> if no credentials are
	 *         available.
	 * @throws NoSuchAlgorithmException
	 *             If the trust manager cannot be set up because of algorithm
	 *             problems.
	 * @throws KeyStoreException
	 *             If the trust manager cannot be set up because of problems
	 *             with the keystore type.
	 * @throws CertificateException
	 *             If a bad certificate is present in the default keystore;
	 *             <i>should be impossible</i>.
	 * @throws IOException
	 *             If problems happen when trying to contact the service.
	 * @throws KeyManagementException
	 *             If the SSL context can't have its special context manager
	 *             installed.
	 */
	private X509Certificate[] getCertificateChainForService(String host,
			int port) throws NoSuchAlgorithmException, KeyStoreException,
			CertificateException, IOException, KeyManagementException {
		KeyStore ks = KeyStore.getInstance(keystoreType);
		SSLContext context = SSLContext.getInstance(protocol);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
		ks.load(null, null);
		tmf.init(ks);
		final Holder<X509Certificate[]> chain = new Holder<X509Certificate[]>();
		final X509TrustManager defaultTrustManager = (X509TrustManager) tmf
				.getTrustManagers()[0];
		context.init(null, new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] clientChain,
					String authType) throws CertificateException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void checkServerTrusted(X509Certificate[] serverChain,
					String authType) throws CertificateException {
				chain.value = serverChain;
				defaultTrustManager.checkServerTrusted(serverChain, authType);
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				throw new UnsupportedOperationException();
			}
		} }, null);
		SSLSocketFactory factory = context.getSocketFactory();
		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(timeout);
		try {
			socket.startHandshake();
			socket.close();
		} catch (SSLException e) {
			// Ignore
		}
		return chain.value;
	}

	private Map<URI, List<X509Certificate>> cache = new HashMap<URI, List<X509Certificate>>();

	/**
	 * Gets the certificate chain for a service identified by URI.
	 * 
	 * @param uri
	 *            The URI of the (secure) service to identify.
	 * @return The certificate chain. Will be <tt>null</tt> if the service is
	 *         not secure.
	 * @throws IOException
	 *             If the service is unreachable or other connection problems
	 *             occur.
	 * @throws GeneralSecurityException
	 *             If any of a number of security-related problems occur, such
	 *             as an inability to match detailed security protocols.
	 */
	public List<X509Certificate> getTrustsForURI(URI uri) throws IOException,
			GeneralSecurityException {
		if (!secure)
			return null;
		synchronized (this) {
			if (!cache.containsKey(uri)) {
				X509Certificate[] chain = getCertificateChainForService(
						uri.getHost(), uri.getPort());
				if (chain != null)
					cache.put(uri, unmodifiableList(asList(chain)));
				else
					cache.put(uri, null);
			}
			return cache.get(uri);
		}
	}

	/**
	 * Flushes the cache.
	 */
	public void flushCache() {
		synchronized (this) {
			cache.clear();
		}
	}
}
