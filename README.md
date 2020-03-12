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


## Taverna Project Retired

> tl;dr: The Taverna code base is **no longer maintained** 
> and is provided here for archival purposes.

From 2014 till 2020 this code base was maintained by the 
[Apache Incubator](https://incubator.apache.org/) project _Apache Taverna (incubating)_
(see [web archive](https://web.archive.org/web/20200312133332/https://taverna.incubator.apache.org/)
and [podling status](https://incubator.apache.org/projects/taverna.html)).

In 2020 the Taverna community 
[voted](https://lists.apache.org/thread.html/r559e0dd047103414fbf48a6ce1bac2e17e67504c546300f2751c067c%40%3Cdev.taverna.apache.org%3E)
to **retire** Taverna as a project and withdraw the code base from the Apache Software Foundation. 

This code base remains available under the Apache License 2.0 
(see _License_ below), but is now simply called 
_Taverna_ rather than ~~Apache Taverna (incubating)~~.

While the code base is no longer actively maintained, 
Pull Requests are welcome to the 
[GitHub organization taverna](http://github.com/taverna/), 
which may infrequently be considered by remaining 
volunteer caretakers.


### Previous releases

Releases 2015-2018 during incubation at Apache Software Foundation
are available from the ASF Download Archive <https://archive.apache.org/dist/incubator/taverna/>

Releases 2014 from the University of Manchester are on BitBucket <https://bitbucket.org/taverna/>

Releases 2009-2013 from myGrid are on LaunchPad <https://launchpad.net/taverna/>

Releases 2003-2009 are on SourceForge <https://sourceforge.net/projects/taverna/files/taverna/>

Binary JARs for Taverna are available from 
Maven Central <https://repo.maven.apache.org/maven2/org/apache/taverna/>
or the myGrid Maven repository <https://repository.mygrid.org.uk/>



# Taverna Server

REST/WSDL web service for executing
[Taverna](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/)
workflows.



## License

* (c) 2007-2014 University of Manchester
* (c) 2014-2020 Apache Software Foundation

This product includes software developed at The [Apache Software
Foundation](https://www.apache.org/).

Licensed under the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), see the file
[LICENSE](LICENSE) for details.

The file [NOTICE](NOTICE) contain any additional attributions and
details about embedded third-party libraries and source code.



# Contribute

Any contributions received are assumed to be covered by the 
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). 



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
Taverna project, you may not want to run the
[Rat Maven plugin](https://creadur.apache.org/rat/apache-rat-plugin/)
that enforces Apache headers in every source file - to disable it, try:

    mvn clean install -Drat.skip=true

# Documentation

https://taverna.incubator.apache.org/documentation/server/ 

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
  [Taverna Command-line Tool](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/commandline/)
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
  [Taverna Language](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/language/),
  [Taverna OSGi](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/osgi/),
  [Taverna Engine](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/engine/), 
  [Taverna Common Activities](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/common-activities/),
  and [Taverna Command-line Tool](https://web.archive.org/web/*/https://taverna.incubatorT.apache.org/download/commandline/).

