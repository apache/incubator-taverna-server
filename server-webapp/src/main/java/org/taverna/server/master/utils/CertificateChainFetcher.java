package org.taverna.server.master.utils;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

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

	private String protocol = "TLS";
	private String keystoreType = KeyStore.getDefaultType();
	private String algorithm = TrustManagerFactory.getDefaultAlgorithm();
	private int timeout = 10000;

	private X509Certificate[] getCertificateChainForService(String host, int port)
			throws NoSuchAlgorithmException, KeyStoreException,
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

	private Map<URI,List<X509Certificate>> cache = new HashMap<URI,List<X509Certificate>>();

	public List<X509Certificate> getTrustsForURI(URI uri) throws IOException, GeneralSecurityException {
		synchronized(this) {
			if (!cache.containsKey(uri))
				cache.put(uri, Collections.unmodifiableList(Arrays.asList(getCertificateChainForService(uri.getHost(), uri.getPort()))));
			return cache.get(uri);
		}
	}

	public void flushCache() {
		cache.clear();
	}
}
