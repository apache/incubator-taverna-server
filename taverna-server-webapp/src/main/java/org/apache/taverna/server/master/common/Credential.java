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
import java.net.URI;
import java.security.Key;
import java.security.cert.Certificate;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import javax.annotation.Nonnull;

/**
 * A description of a private credential. This description is characterised by a
 * file visible to the workflow run that contains a particular key-pair.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "CredentialDescriptor")
@XmlSeeAlso({ Credential.KeyPair.class, Credential.Password.class })
@SuppressWarnings("serial")
public abstract class Credential implements Serializable {
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
	@XmlSchemaType(name = "anyURI")
	public URI serviceURI;
	/** The key extracted from the keystore. */
	public transient Key loadedKey;
	/** The trust chain of the key extracted from the keystore. */
	public transient Certificate[] loadedTrustChain;

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public final boolean equals(Object o) {
		if (o == null || !(o instanceof Credential))
			return false;
		return equals((Credential) o);
	}

	protected boolean equals(@Nonnull Credential c) {
		return id.equals(c.id);
	}

	/**
	 * A description of a credential that is a public/private key-pair in some
	 * kind of key store.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "keypair")
	@XmlType(name = "KeyPairCredential")
	public static class KeyPair extends Credential {
		/** The name of the credential within its store, i.e., it's alias. */
		@XmlElement(required = true)
		public String credentialName;
		/**
		 * The keystore file containing the credential. This is resolved with
		 * respect to the workflow run working directory.
		 */
		@XmlElement
		public String credentialFile;
		/**
		 * The type of keystore file. Defaults to <tt>JKS</tt> if unspecified.
		 */
		@XmlElement
		public String fileType;
		/**
		 * The password used to unlock the keystore file. It is assumed that the
		 * same password is used for unlocking the credential within, or that
		 * the inner password is empty.
		 */
		@XmlElement
		public String unlockPassword;
		/**
		 * The encoded serialized keystore containing the credential.
		 */
		@XmlElement
		public byte[] credentialBytes;

		@Override
		public String toString() {
			return "keypair(id=" + id + ")";
		}
	}

	/**
	 * A description of a credential that is a username and password.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "userpass")
	@XmlType(name = "PasswordCredential")
	public static class Password extends Credential {
		@XmlElement(required = true)
		public String username;
		@XmlElement(required = true)
		public String password;

		@Override
		public String toString() {
			return "userpass(id=" + id + ")";
		}
	}

	/**
	 * A credential that is just used for deleting credentials by ID. Cannot be
	 * marshalled as XML.
	 * 
	 * @author Donal Fellows
	 */
	public static class Dummy extends Credential {
		public Dummy(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "dummy(id=" + id + ")";
		}
	}
}