/*
 * Copyright (C) 2011-2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.common;

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