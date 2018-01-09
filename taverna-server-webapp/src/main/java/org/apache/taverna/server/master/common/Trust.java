/*
 */
package org.taverna.server.master.common;
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

import static org.taverna.server.master.common.Namespaces.XLINK;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A description of a trusted identity or identities. This description is
 * characterised by a file visible to the workflow run that contains one or more
 * certificates.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "TrustDescriptor")
@XmlRootElement(name = "trustedIdentity")
@SuppressWarnings("serial")
public final class Trust implements Serializable {
	/** The location of this descriptor in the REST world. */
	@XmlAttribute(namespace = XLINK)
	public String href;
	/**
	 * The location of this descriptor in the SOAP world. Must match corrected
	 * with the {@link #href} field.
	 */
	@XmlTransient
	public String id;
	/**
	 * The file containing the certificate(s). This is resolved with respect to
	 * the workflow run working directory.
	 */
	@XmlElement
	public String certificateFile;
	/**
	 * The type of certificate file. Defaults to <tt>X.509</tt> if unspecified.
	 */
	@XmlElement
	public String fileType;
	/**
	 * The encoded serialized keystore containing the certificate(s).
	 */
	@XmlElement
	public byte[] certificateBytes;
	/**
	 * The names of the server(s) identified by this trust.
	 */
	@XmlElement
	public List<String> serverName;
	/**
	 * The collection of certificates loaded from the specified file. This is
	 * always <tt>null</tt> before validation.
	 */
	public transient Collection<? extends Certificate> loadedCertificates;

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Trust))
			return false;
		return id.equals(((Trust) o).id);
	}
}