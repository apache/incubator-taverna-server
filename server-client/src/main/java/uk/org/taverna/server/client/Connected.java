package uk.org.taverna.server.client;

import uk.org.taverna.server.client.TavernaServer.ClientException;
import uk.org.taverna.server.client.TavernaServer.ServerException;

import com.sun.jersey.api.client.ClientResponse;

abstract class Connected {
	void checkError(ClientResponse response) throws ClientException,
			ServerException {
		ClientResponse.Status s = response.getClientResponseStatus();
		if (s.getStatusCode() == 401)
			throw new TavernaServer.AuthorizationException("not authorized",
					null);
		if (s.getStatusCode() >= 500)
			throw new TavernaServer.ServerException(s.getReasonPhrase(), null);
		if (s.getStatusCode() >= 400)
			throw new TavernaServer.ClientException(s.getReasonPhrase(), null);
	}
}
