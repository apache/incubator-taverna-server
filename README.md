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
[Apache Taverna](http://taverna.incubator.apache.org/) (incubating)
workflows.



## License

(c) 2007-2014 University of Manchester
(c) 2015 Apache Software Foundation

This product includes software developed at The [Apache Software
Foundation](http://www.apache.org/).

Licensed under the
[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), see the file
[LICENSE](LICENSE) for details.

The file [NOTICE](NOTICE) contain any additional attributions and
details about embedded third-party libraries and source code.



# Contribute

Please subscribe to and contact the
[dev@taverna](http://taverna.incubator.apache.org/community/lists#dev mailing list)
for any questions, suggestions and discussions about
Apache Taverna.

Bugs and feature plannings are tracked in the Jira
[Issue tracker](https://issues.apache.org/jira/browse/TAVERNA/component/12326813)
under the `TAVERNA` component _Taverna Server_. Feel free
to add an issue!

To suggest changes to this source code, feel free to raise a
[GitHub pull request](https://github.com/apache/incubator-taverna-service/pulls).
Any contributions received are assumed to be covered by the [Apache License
2.0](https://www.apache.org/licenses/LICENSE-2.0). We might ask you
to sign a [Contributor License Agreement](https://www.apache.org/licenses/#clas)
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



## Prerequisites

* Java 1.7 or newer (tested with OpenJDK 1.8)
* [Apache Maven](https://maven.apache.org/download.html) 3.2.5 or newer (older
  versions probably also work)


# Building

To build, use

    mvn clean install

This will build each module and run their tests.


## Skipping tests

To skip the tests (these can be timeconsuming), use:

    mvn clean install -DskipTests


If you are modifying this source code independent of the
Apache Taverna project, you may not want to run the
[Rat Maven plugin](https://creadur.apache.org/rat/apache-rat-plugin/)
that enforces Apache headers in every source file - to disable it, try:

    mvn clean install -Drat.skip=true


## A Beginner's Installation Guide to Taverna Server

When installing Taverna Server 2.5, you *need* to decide whether to
install in secure or insecure mode. In secure mode, the server
enforces logins, ensures that they are done over HTTPS, and applies
strong restrictions to what users can see of other users'
workflows. In insecure mode, no restrictions are enforced which
simplifies configuration but greatly reduces the overall system
security. *Do not mix up installations between the two types.*

You will need:

* **Unix** (e.g., Linux, OSX). Running Linux inside a virtual machine
  works. Running directly on Windows is not supported.

* **Java 7** (or later) installed. See the Java requirements on the
  [Taverna website](http://www.taverna.org.uk/download/workbench/system-requirements/).

* **Tomcat 6** (recent version).

* **Taverna Server 2.5**. Either the "full installation" or WAR will do
  (the "full installation" zip contains a copy of the WAR) - see the
  [Taverna website](http://www.taverna.org.uk/download/server/2-4/) for details on downloading the file.

If you are installing in secured mode (default) you will also need:

* **SSL** (i.e., HTTPS) **host certificate**. This should not be
  self-signed and should have the hostname in the Common Name (CN)
  field. (Self-signed certificates or ones without the hostname in are
  exceptionally awkward for clients to work with, and proper
  single-host certificates are in reality very cheap. Save yourself a
  lot of work here!)

* For the simplest operation, you should create a user `taverna` that is
  a member of the group called `taverna`. This user will be used for
  executing workflows, and does not need to allow anyone to log in as
  it.

Stick to the Factory Defaults
-----------------------------

Taverna Server 2.5 has a long list of things that may be configured,
but it comes with “factory” settings that are correct in the majority
of cases. Leave them alone for your first installation.

Setting up Tomcat
-----------------

Note that the instructions below do not describe setting up Tomcat
users. These are not necessary for Taverna Server, as that needs
finer-grained permission control than is normal for a webapp.

You can always find further information by searching the web for
“_install tomcat6 YourOperatingSystem_”.

### Installing on Debian Linux, Ubuntu

On Debian Linux (and derivatives), you install Tomcat with:

    sudo apt-get install tomcat6 tomcat6-admin tomcat6-common tomcat6-user

You then start Tomcat with:

    sudo /etc/init.d/tomcat6 start

And stop it with:

    sudo /etc/init.d/tomcat6 stop

It's configuration file (called `conf/server.xml` in the instructions below) will be in:

    /etc/tomcat6/server.xml

It's webapp directory (`webapps` below) will be in:

    /var/lib/tomcat6

### Installing on RedHat Linux, Fedora, CentOS, Scientific Linux

On RedHat Linux (and derivatives), you install Tomcat with:

    yum install tomcat6-webapps

You then start Tomcat with:

    sudo service tomcat6 start

And stop it with:

    sudo service tomcat6 stop

It's configuration file (called `conf/server.xml` in the instructions below) will be in:

    /etc/tomcat6/server.xml

It's webapp directory (`webapps` below) will be in:

    /var/lib/tomcat6

### Installing on MacOS X, and using a baseline Apache distribution

On OSX (or if otherwise installing from a standard Apache
distribution), you install Tomcat by downloading from the distribution
page at:

* http://tomcat.apache.org/download-60.cgi

Both ZIP and `.tar.gz` download versions include a file `RUNNING.txt`
that describes how to perform the installation, start the server, and
stop it again.

The normal location of the configuration file (`conf/server.xml` in
the instructions below) is, for Tomcat 6.0.35:

    /usr/local/tomcat6.0/apache-tomcat-6.0.35/conf/server.xml

And its `webapps` directory is at:

    /usr/local/tomcat6.0/apache-tomcat-6.0.35/webapps

Installing an Unsecured Taverna Server
--------------------------------------

This is not the default configuration of Taverna Server because it is
_insecure_; there is no attempt to verify the identity of users or to
keep them from interfering with each other's workflows. _We recommend
that you use the secured version if possible._

The insecure version is installed by:

### First, place the WAR into Tomcat's webapps directory

Use a filename that relates to what URL you want Taverna Server to
appear at within Tomcat (e.g., if you want it to be at
`/tavernaserver`, use the filename `webapps/tavernaserver.war`).

### Next, start Tomcat (if stopped), and shut it down again once it has unpacked the WAR.

At this point, Taverna Server is installed but not usable.

### Then configure for unsecure operation.

Go to the unpacked WAR, find its `WEB-INF/web.xml` (with the above
installation path, it would be
`webapps/tavernaserver/WEB-INF/web.xml`), and change the lines:

    <param-value>WEB-INF/secure.xml</param-value>
    <!-- <param-value>WEB-INF/insecure.xml</param-value> -->

to read:

    <!-- <param-value>WEB-INF/secure.xml</param-value> -->
    <param-value>WEB-INF/insecure.xml</param-value>

This changes which part of the rest of the server configuration is
used. It does so by altering what part of that XML file are commented
out. One of those two `<param-value>` lines **must** be
uncommented. The overall XML file **must** be valid.

### Finally, start Tomcat.

> **NB:** When accessing an unsecured Taverna Server, for most
    operations (such as submitting a run) you will need to pass the
    credentials for the default user. The default user has username
    `taverna` and password `taverna`.

Installing a Secured Taverna Server
-----------------------------------
Taverna Server 2.5 is installed in secure mode by doing this:

### First you need to enable SSL on Tomcat.

With Tomcat not running, make sure that its `conf/server.xml` file
contains a `<Connector>` definition for SSL HTTP/1.1. The file should
contain comments on how to do this. Here's an example:

    <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
        maxThreads="150" scheme="https" secure="true"
        clientAuth="false" sslProtocol="TLS" keystorePass="tomcat"
        keystoreFile="conf/tavserv.p12" keystoreType="PKCS12" />

This configuration enables secure access on port 8443 (HTTPS-alt;
strongly recommended) with the server using the key-pair that has been
placed in a standard PKCS #12 format file in the file `tavserv.p12` in
the same directory as the configuration file; the key-pair file will
be unlocked with the (rather obvious) password “`tomcat`”.

Note that if the configuration file is located below `/etc`, it is
recommended that you specify the full path to the PKCS #12 file. You
should also ensure that the file can only be read by the Unix user
that will be running Tomcat.

### Next, you need to grant permission to the Tomcat server to run code as other users.

In particular, it needs to be able to run the Java executable it is
using as other people via `sudo`. You _should_ take care to lock this
down heavily. You do this by using the program visudo to add these
parts to the sudo configuration. Note that each goes in a separate
part of the overall file, and that we assume below that Tomcat is
running as the user `tavserv`; you will probably need to change (e.g.,
to `tomcat` or `nobody`) as appropriate.

This defines some flags for the main server user:

    Defaults:tavserv   !lecture, timestamp_timeout=0, passwd_tries=1

This defines a rule for who the server can switch to. Let's say that
they have to be a member of the Unix user group `taverna`; if a user
isn't in that group, they cannot use Taverna Server. (Note that `root`
should not be part of the group!)

    Runas_Alias        TAV = %taverna

This creates the actual permission, saying that the `tavserv` user may
run anything as any user in the alias above (i.e., in the `taverna`
group). The `NOPASSWD` is important because it allows Taverna Server
to do the delegation even when running as a user that can't log in.

    tavserv            ALL=(TAV) NOPASSWD: ALL

### Now, place the WAR into Tomcat's `webapps` directory.

Use a filename that relates to what URL you want Taverna Server to
appear at within Tomcat (e.g., if you want it to be at
`/tavernaserver`, use the filename `webapps/tavernaserver.war`).

### Finally, start Tomcat.
