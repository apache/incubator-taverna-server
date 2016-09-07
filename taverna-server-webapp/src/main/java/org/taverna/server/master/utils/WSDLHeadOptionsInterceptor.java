package org.taverna.server.master.utils;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.apache.commons.logging.LogFactory.getLog;
import static org.apache.cxf.common.util.UrlUtils.parseQueryString;
import static org.apache.cxf.message.Message.HTTP_REQUEST_METHOD;
import static org.apache.cxf.message.Message.QUERY_STRING;
import static org.apache.cxf.message.Message.REQUEST_URL;
import static org.apache.cxf.phase.Phase.READ;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.cxf.binding.soap.interceptor.EndpointSelectionInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;


/**
 * Thunk for TAVSERV-293.
 * 
 * @author Donal Fellows (based on work by Daniel Hagen)
 */
public class WSDLHeadOptionsInterceptor extends
		AbstractPhaseInterceptor<Message> {
	public static final Log log = getLog("Taverna.Server.Utils");

	public WSDLHeadOptionsInterceptor() {
		super(READ);
		getAfter().add(EndpointSelectionInterceptor.class.getName());
	}

	@Override
	public void handleMessage(Message message) throws Fault {
		String method = (String) message.get(HTTP_REQUEST_METHOD);
		String query = (String) message.get(QUERY_STRING);

		if (("HEAD".equals(method) || "OPTIONS".equals(method))
				&& query != null && !query.trim().isEmpty()
				&& isRecognizedQuery(query)) {
			log.debug("adjusting message request method " + method + " for "
					+ message.get(REQUEST_URL) + " to GET");
			message.put(HTTP_REQUEST_METHOD, "GET");
		}
	}

	/*
	 * Stolen from http://permalink.gmane.org/gmane.comp.apache.cxf.user/20037
	 * which is itself in turn stolen from
	 * org.apache.cxf.frontend.WSDLGetInterceptor.isRecognizedQuery
	 */
	/**
	 * Is this a query for WSDL or XSD relating to it?
	 * 
	 * @param query
	 *            The query string to check
	 * @return If the query is one to handle.
	 * @see org.apache.cxf.frontend.WSDLGetInterceptor#isRecognizedQuery(Map,String,String,org.apache.cxf.service.model.EndpointInfo)
	 *      WSDLGetInterceptor
	 */
	private boolean isRecognizedQuery(String query) {
		Map<String, String> map = parseQueryString(query);
		return map.containsKey("wsdl") || map.containsKey("xsd");
	}
}
