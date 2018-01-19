<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
# Apache Taverna Server (incubating)

REST/WSDL web service for executing
[Apache Taverna](https://taverna.incubator.apache.org/) (incubating)
workflows.



## License

* (c) 2007-2014 University of Manchester
* (c) 2014-2018 Apache Software Foundation

This product includes software developed at The [Apache Software
Foundation](http://www.apache.org/).

Licensed under the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), see the file
[LICENSE](LICENSE) for details.

The file [NOTICE](NOTICE) contain any additional attributions and
details about embedded third-party libraries and source code.



# Contribute

Please subscribe to and contact the
[dev@taverna](https://taverna.incubator.apache.org/community/lists#dev) mailing list
for any questions, suggestions and discussions about
Apache Taverna.

Bugs and feature plannings are tracked in the Jira
[Issue tracker](https://issues.apache.org/jira/browse/TAVERNA/component/12326813)
under the `TAVERNA` component _Taverna Server_. Feel free
to add an issue!

To suggest changes to this source code, feel free to raise a
[GitHub pull request](https://github.com/apache/incubator-taverna-server/pulls).
Any contributions received are assumed to be covered by the 
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). 
We might ask you to sign a 
[Contributor License Agreement](https://www.apache.org/licenses/#clas)
before accepting a larger contribution.


## Disclaimer

Apache Taverna is an effort undergoing incubation at the
[Apache Software Foundation (ASF)](http://www.apache.org/),
sponsored by the [Apache Incubator PMC](http://incubator.apache.org/).

[Incubation](http://incubator.apache.org/incubation/Process_Description.html)
is required of all newly accepted projects until a further review
indicates that the infrastructure, communications, and decision making process
have stabilized in a manner consistent with other successful ASF projects.

While incubation status is not necessarily a reflection of the completeness
or stability of the code, it does indicate that the project has yet to be
fully endorsed by the ASF.



## Build prerequisites

* Java 1.8
* [Apache Maven](https://maven.apache.org/download.html) 3.2.5 or newer (older
  versions probably also work)


# Building

To build, use

    mvn clean install

This will build each module and run their tests.

You should then find `taverna-server.war` in the folder
`taverna-server-webapp/target/`


## Skipping tests

To skip the tests (these can be timeconsuming), use:

    mvn clean install -DskipTests


If you are modifying this source code independent of the
Apache Taverna project, you may not want to run the
[Rat Maven plugin](https://creadur.apache.org/rat/apache-rat-plugin/)
that enforces Apache headers in every source file - to disable it, try:

    mvn clean install -Drat.skip=true

More information:

 * [Introduction](introduction.md)
 * [Installing](install.md)
 * [Using](usage.md)


# Export restrictions

This distribution includes cryptographic software.
The country in which you currently reside may have restrictions 
on the import, possession, use, and/or re-export to another country,
of encryption software. BEFORE using any encryption software,
please check your country's laws, regulations and policies
concerning the import, possession, or use, and re-export of
encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS),
has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1,
which includes information security software using or performing
cryptographic functions with asymmetric algorithms.
The form and manner of this Apache Software Foundation distribution makes
it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception
(see the BIS Export Administration Regulations, Section 740.13)
for both object code and source code.

The following provides more details on the included cryptographic software:

* Taverna Server's `CertificateChainFetcher` uses 
  [Java Secure Socket Extension](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
  (JSS) to pre-fetch certificates of SSL-secured web services accessed by Taverna workflows.
* Taverna Server's support for propagating username/password credentials in
  `SecurityContextFactory` relies on 
  [BouncyCastle](https://www.bouncycastle.org/) bcprov encryption library and
  [Java Cryptography Extension](http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
  (JCE) to generate a keystore for Taverna Command-line tool.
  The [JCE Unlimited Strength Jurisdiction Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
  may need to be installed separately.
* Taverna Server may interact with the credential manager support in
  [Apache Taverna Command-line Tool](https://taverna.incubator.apache.org/download/commandline/)
  to provide a keystore of client credentials and trusted certificates for SSL-secured web services.
* After building, the 
  `taverna-server-webapp/target/taverna-server.war` will include 
  dependencies that are covered
  by export restrictions, including:  
  [BouncyCastle](https://www.bouncycastle.org/) bcprov encryption library,
  [Apache HttpComponents](https://hc.apache.org/) Core and Client,
  [Apache Derby](http://db.apache.org/derby/),
  [Jetty](http://www.eclipse.org/jetty/),
  [Apache WSS4J](https://ws.apache.org/wss4j/),
  [Apache XML Security for Java](https://santuario.apache.org/javaindex.html),
  [Open SAML Java](https://shibboleth.net/products/opensaml-java.html),
  [Apache Taverna Language](https://taverna.incubator.apache.org/download/language/),
  [Apache Taverna OSGi](https://taverna.incubator.apache.org/download/osgi/),
  [Apache Taverna Engine](https://taverna.incubator.apache.org/download/engine/), 
  [Apache Taverna Common Activities](https://taverna.incubator.apache.org/download/common-activities/),
  and [Apache Taverna Command-line Tool](https://taverna.incubator.apache.org/download/commandline/).


# Export restrictions

This distribution includes cryptographic software.
The country in which you currently reside may have restrictions 
on the import, possession, use, and/or re-export to another country,
of encryption software. BEFORE using any encryption software,
please check your country's laws, regulations and policies
concerning the import, possession, or use, and re-export of
encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS),
has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1,
which includes information security software using or performing
cryptographic functions with asymmetric algorithms.
The form and manner of this Apache Software Foundation distribution makes
it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception
(see the BIS Export Administration Regulations, Section 740.13)
for both object code and source code.

The following provides more details on the included cryptographic software:

* Taverna Server's `CertificateChainFetcher` uses 
  [Java Secure Socket Extension](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html)
  (JSS) to pre-fetch certificates of SSL-secured web services accessed by Taverna workflows.
* Taverna Server's support for propagating username/password credentials in
  `SecurityContextFactory` relies on 
  [BouncyCastle](https://www.bouncycastle.org/) bcprov encryption library and
  [Java Cryptography Extension](http://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html)
  (JCE) to generate a keystore for Taverna Command-line tool.
  The [JCE Unlimited Strength Jurisdiction Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)
  may need to be installed separately.
* Taverna Server may interact with the credential manager support in
  [Apache Taverna Command-line Tool](https://taverna.incubator.apache.org/download/commandline/)
  to provide a keystore of client credentials and trusted certificates for SSL-secured web services.
* After building, the 
  `taverna-server-webapp/target/taverna-server.war` will include 
  dependencies that are covered
  by export restrictions, including:  
  [BouncyCastle](https://www.bouncycastle.org/) bcprov encryption library,
  [Apache HttpComponents](https://hc.apache.org/) Core and Client,
  [Apache Derby](http://db.apache.org/derby/),
  [Jetty](http://www.eclipse.org/jetty/),
  [Apache WSS4J](https://ws.apache.org/wss4j/),
  [Apache XML Security for Java](https://santuario.apache.org/javaindex.html),
  [Open SAML Java](https://shibboleth.net/products/opensaml-java.html),
  [Apache Taverna Language](https://taverna.incubator.apache.org/download/language/),
  [Apache Taverna OSGi](https://taverna.incubator.apache.org/download/osgi/),
  [Apache Taverna Engine](https://taverna.incubator.apache.org/download/engine/), 
  [Apache Taverna Common Activities](https://taverna.incubator.apache.org/download/common-activities/),
  and [Apache Taverna Command-line Tool](https://taverna.incubator.apache.org/download/commandline/).
