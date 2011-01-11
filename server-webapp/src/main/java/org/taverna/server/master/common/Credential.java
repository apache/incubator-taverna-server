/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.common;

import static org.taverna.server.master.common.Namespaces.XLINK;

import java.net.URI;
import java.security.Key;
import java.security.cert.Certificate;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A description of a private credential. This description is characterised by a
 * file visible to the workflow run that contains a particular key-pair.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "CredentialDescriptor")
@XmlRootElement(name = "credential")
public final class Credential {
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
	 * The service URI to use this credential with. If omitted, this represents
	 * the <i>default</i> credential to use.
	 */
	@XmlElement
	public URI serviceURI;
	/** The name of the credential within its store, i.e., it's alias. */
	@XmlElement(required = true)
	public String credentialName;
	/**
	 * The type of credential being presented. Should be one of <tt>key</tt> or
	 * <tt>password</tt>; other types may be supported in the future.
	 */
	@XmlElement(required = true)
	public String credentialType;
	/**
	 * The keystore file containing the credential. This is resolved with
	 * respect to the workflow run working directory.
	 */
	@XmlElement(required = true)
	public String credentialFile;
	/**
	 * The type of keystore file. Defaults to <tt>JKS</tt> if unspecified.
	 */
	@XmlElement
	public String fileType;
	/**
	 * The password used to unlock the keystore file. It is assumed that the
	 * same password is used for unlocking the credential within, or that the
	 * inner password is empty.
	 */
	@XmlElement
	public String unlockPassword;
	/** The key extracted from the keystore. */
	@XmlTransient
	public Key loadedKey;
	/** The trust chain of the key extracted from the keystore. */
	@XmlTransient
	public Certificate[] loadedTrustChain;

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Credential))
			return false;
		return id.equals(((Credential) o).id);
	}
}