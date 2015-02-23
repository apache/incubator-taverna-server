package uk.org.taverna.server.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class TavernaServerConnectionFactory {
	private Map<URI, TavernaServer> cache = new HashMap<>();

	public synchronized TavernaServer connectNoAuth(URI uri) {
		TavernaServer conn = cache.get(uri);
		if (conn == null)
			cache.put(uri, conn = new TavernaServer(uri));
		return conn;
	}

	public TavernaServer connectAuth(URI uri, String username, String password) {
		TavernaServer conn = new TavernaServer(uri, username, password);
		// Force a check of the credentials by getting the server version
		conn.getServerVersionInfo();
		return conn;
	}
}
