/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.admin;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.ogf.usage.JobUsageRecord;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The administration interface for Taverna Server.
 * 
 * @author Donal Fellows
 */
@Description("Administration interface for Taverna Server.")
public interface Admin {
	/**
	 * Get a simple administration user interface.
	 * 
	 * @return The description document in a response.
	 * @throws IOException
	 */
	@GET
	@Path("/")
	@Produces("text/html")
	@NonNull
	Response getUserInterface() throws IOException;

	/**
	 * Gets support resources for the administration user interface.
	 * 
	 * @param file
	 *            The name of the static resource to provide.
	 * @return The requested document in a response.
	 * @throws IOException
	 */
	@GET
	@Path("static/{file}")
	@Produces("*/*")
	Response getStaticResource(@PathParam("file") String file)
			throws IOException;

	/**
	 * Get a description of the adminstration interface.
	 * 
	 * @param ui
	 *            What URI was used to access this resource?
	 * @return The description document.
	 */
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json" })
	@NonNull
	AdminDescription getDescription(@Context UriInfo ui);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("/")
	Response optionsRoot();

	/**
	 * Get whether to allow new workflow runs to be created.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("allowNew")
	@Produces("text/plain")
	@Description("Whether to allow new workflow runs to be created.")
	boolean getAllowNew();

	/**
	 * Set whether to allow new workflow runs to be created.
	 * 
	 * @param newValue
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("allowNew")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Whether to allow new workflow runs to be created.")
	boolean setAllowNew(boolean newValue);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("allowNew")
	@Description("Whether to allow new workflow runs to be created.")
	Response optionsAllowNew();

	/**
	 * Get whether to log the workflows submitted.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("logWorkflows")
	@Produces("text/plain")
	@Description("Whether to log the workflows submitted.")
	boolean getLogWorkflows();

	/**
	 * Set whether to log the workflows submitted.
	 * 
	 * @param logWorkflows
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("logWorkflows")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Whether to log the workflows submitted.")
	boolean setLogWorkflows(boolean logWorkflows);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("logWorkflows")
	@Description("Whether to log the workflows submitted.")
	Response optionsLogWorkflows();

	/**
	 * Get whether to log the user-directed faults.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("logFaults")
	@Produces("text/plain")
	@Description("Whether to log the user-directed faults.")
	boolean getLogFaults();

	/**
	 * Set whether to log the user-directed faults.
	 * 
	 * @param logFaults
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("logFaults")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Whether to log the user-directed faults.")
	boolean setLogFaults(boolean logFaults);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("logFaults")
	@Description("Whether to log the user-directed faults.")
	Response optionsLogFaults();

	/**
	 * Get what file to dump usage records to.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("usageRecordDumpFile")
	@Produces("text/plain")
	@Description("What file to dump usage records to.")
	@NonNull
	String getURFile();

	/**
	 * Set what file to dump usage records to.
	 * 
	 * @param urFile
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("usageRecordDumpFile")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What file to dump usage records to.")
	@NonNull
	String setURFile(@NonNull String urFile);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("usageRecordDumpFile")
	@Description("What file to dump usage records to.")
	Response optionsURFile();

	/**
	 * The property for the number of times the service methods have been
	 * invoked.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("invokationCount")
	@Produces("text/plain")
	@Description("How many times have the service methods been invoked.")
	int invokeCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("invokationCount")
	@Description("How many times have the service methods been invoked.")
	Response optionsInvokationCount();

	/**
	 * The property for the number of runs that are currently in existence.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("runCount")
	@Produces("text/plain")
	@Description("How many runs are currently in existence.")
	int runCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("runCount")
	@Description("How many runs are currently in existence.")
	Response optionsRunCount();

	/**
	 * The property for the number of runs that are currently running.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("operatingCount")
	@Produces("text/plain")
	@Description("How many runs are currently actually running.")
	int operatingCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("operatingCount")
	@Description("How many runs are currently actually running.")
	Response optionsOperatingCount();

	/**
	 * Get the location of the RMI registry.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("registryHost")
	@Produces("text/plain")
	@Description("Where is the RMI registry?")
	@NonNull
	String getRegistryHost();

	/**
	 * Set the location of the RMI registry.
	 * 
	 * @param registryHost
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("registryHost")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Where is the RMI registry?")
	@NonNull
	String setRegistryHost(@NonNull String registryHost);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("registryHost")
	@Description("Where is the RMI registry?")
	Response optionsRegistryHost();

	/**
	 * Get the port of the RMI registry.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("registryPort")
	@Produces("text/plain")
	@Description("On what port is the RMI registry?")
	int getRegistryPort();

	/**
	 * Set the port of the RMI registry.
	 * 
	 * @param registryPort
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("registryPort")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("On what port is the RMI registry?")
	int setRegistryPort(int registryPort);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("registryPort")
	@Description("On what port is the RMI registry?")
	Response optionsRegistryPort();

	/**
	 * Get the maximum number of simultaneous runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("runLimit")
	@Produces("text/plain")
	@Description("What is the maximum number of simultaneous runs?")
	int getRunLimit();

	/**
	 * Set the maximum number of simultaneous runs.
	 * 
	 * @param runLimit
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("runLimit")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the maximum number of simultaneous runs?")
	int setRunLimit(int runLimit);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("runLimit")
	@Description("What is the maximum number of simultaneous runs?")
	Response optionsRunLimit();

	/**
	 * Get the maximum number of simultaneous executing runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("operatingLimit")
	@Produces("text/plain")
	@Description("What is the maximum number of simultaneous executing runs?")
	int getOperatingLimit();

	/**
	 * Set the maximum number of simultaneous executing runs.
	 * 
	 * @param operatingLimit
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("operatingLimit")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the maximum number of simultaneous executing runs?")
	int setOperatingLimit(int operatingLimit);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("operatingLimit")
	@Description("What is the maximum number of simultaneous executing runs?")
	Response optionsOperatingLimit();

	/**
	 * Get the default lifetime of workflow runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("defaultLifetime")
	@Produces("text/plain")
	@Description("What is the default lifetime of workflow runs, in seconds?")
	int getDefaultLifetime();

	/**
	 * Set the default lifetime of workflow runs.
	 * 
	 * @param defaultLifetime
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("defaultLifetime")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the default lifetime of workflow runs, in seconds?")
	int setDefaultLifetime(int defaultLifetime);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("defaultLifetime")
	@Description("What is the default lifetime of workflow runs, in seconds?")
	Response optionsDefaultLifetime();

	/**
	 * The property for the list of IDs of current runs.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("currentRuns")
	@Produces({ "application/xml", "application/json" })
	@Description("List the IDs of all current runs.")
	StringList currentRuns();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("currentRuns")
	@Description("List the IDs of all current runs.")
	Response optionsCurrentRuns();

	/**
	 * Get the Java binary to be used for execution of subprocesses.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("javaBinary")
	@Produces("text/plain")
	@Description("Which Java binary should be used for execution of subprocesses?")
	@NonNull
	String getJavaBinary();

	/**
	 * Set the Java binary to be used for execution of subprocesses.
	 * 
	 * @param javaBinary
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("javaBinary")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Which Java binary should be used for execution of subprocesses?")
	@NonNull
	String setJavaBinary(@NonNull String javaBinary);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("javaBinary")
	@Description("Which Java binary should be used for execution of subprocesses?")
	Response optionsJavaBinary();

	/**
	 * Get the extra arguments to be supplied to Java subprocesses.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("extraArguments")
	@Produces({ "application/xml", "application/json" })
	@Description("What extra arguments should be supplied to Java subprocesses?")
	@NonNull
	StringList getExtraArguments();

	/**
	 * Set the extra arguments to be supplied to Java subprocesses.
	 * 
	 * @param extraArguments
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("extraArguments")
	@Consumes("application/xml")
	@Produces({ "application/xml", "application/json" })
	@Description("What extra arguments should be supplied to Java subprocesses?")
	@NonNull
	StringList setExtraArguments(@NonNull StringList extraArguments);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("extraArguments")
	@Description("What extra arguments should be supplied to Java subprocesses?")
	Response optionsExtraArguments();

	/**
	 * Get the full pathname of the worker JAR file.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("serverWorkerJar")
	@Produces("text/plain")
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	@NonNull
	String getServerWorkerJar();

	/**
	 * Set the full pathname of the worker JAR file.
	 * 
	 * @param serverWorkerJar
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("serverWorkerJar")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	@NonNull
	String setServerWorkerJar(@NonNull String serverWorkerJar);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("serverWorkerJar")
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	Response optionsServerWorkerJar();

	/**
	 * Get the full pathname of the executeWorkflow.sh file.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("executeWorkflowScript")
	@Produces("text/plain")
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	@NonNull
	String getExecuteWorkflowScript();

	/**
	 * Set the full pathname of the executeWorkflow.sh file.
	 * 
	 * @param executeWorkflowScript
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("executeWorkflowScript")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	@NonNull
	String setExecuteWorkflowScript(@NonNull String executeWorkflowScript);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("executeWorkflowScript")
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	Response optionsExecuteWorkflowScript();

	/**
	 * Get the total duration of time to wait for the start of the forker
	 * process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("registrationWaitSeconds")
	@Produces("text/plain")
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	int getRegistrationWaitSeconds();

	/**
	 * Set the total duration of time to wait for the start of the forker
	 * process.
	 * 
	 * @param registrationWaitSeconds
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("registrationWaitSeconds")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	int setRegistrationWaitSeconds(int registrationWaitSeconds);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("registrationWaitSeconds")
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	Response optionsRegistrationWaitSeconds();

	/**
	 * Get the interval between checks for registration of the forker process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("registrationPollMillis")
	@Produces("text/plain")
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	int getRegistrationPollMillis();

	/**
	 * Set the interval between checks for registration of the forker process.
	 * 
	 * @param registrationPollMillis
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("registrationPollMillis")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	int setRegistrationPollMillis(int registrationPollMillis);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("registrationPollMillis")
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	Response optionsRegistrationPollMillis();

	/**
	 * Get the full pathname of the file containing the impersonation
	 * credentials for the forker process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("runasPasswordFile")
	@Produces("text/plain")
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	@NonNull
	String getRunasPasswordFile();

	/**
	 * Set the full pathname of the file containing the impersonation
	 * credentials for the forker process.
	 * 
	 * @param runasPasswordFile
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("runasPasswordFile")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	@NonNull
	String setRunasPasswordFile(@NonNull String runasPasswordFile);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("runasPasswordFile")
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	Response optionsRunasPasswordFile();

	/**
	 * Get the full pathname of the forker's JAR.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path("serverForkerJar")
	@Produces("text/plain")
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	@NonNull
	String getServerForkerJar();

	/**
	 * Set the full pathname of the forker's JAR.
	 * 
	 * @param serverForkerJar
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path("serverForkerJar")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	@NonNull
	String setServerForkerJar(@NonNull String serverForkerJar);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("serverForkerJar")
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	Response optionsServerForkerJar();

	/**
	 * The property for the length of time it took to start the forker.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("startupTime")
	@Produces("text/plain")
	@Description("How long did it take for the back-end \"forker\" to set itself up, in seconds.")
	int startupTime();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("startupTime")
	@Description("How long did it take for the back-end \"forker\" to set itself up, in seconds.")
	Response optionsStartupTime();

	/**
	 * The property for the last exit code of the forker process.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("lastExitCode")
	@Produces("text/plain")
	@Description("What was the last exit code of the \"forker\"? If null, no exit has ever been recorded.")
	Integer lastExitCode();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("lastExitCode")
	@Description("What was the last exit code of the \"forker\"? If null, no exit has ever been recorded.")
	Response optionsLastExitCode();

	/**
	 * The property for the mapping of usernames to factory process handles.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("factoryProcessMapping")
	@Produces({ "application/xml", "application/json" })
	@Description("What is the mapping of local usernames to factory process RMI IDs?")
	StringList factoryProcessMapping();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("factoryProcessMapping")
	@Description("What is the mapping of local usernames to factory process RMI IDs?")
	Response optionsFactoryProcessMapping();

	/**
	 * The property for the list of usage records collected.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path("usageRecords")
	@Produces("application/xml")
	@Description("What is the list of usage records that have been collected?")
	URList usageRecords();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("usageRecords")
	@Description("What is the list of usage records that have been collected?")
	Response optionsUsageRecords();

	@GET
	@Path("users")
	@Produces({ "application/xml", "application/json" })
	@Description("What users are known to the server?")
	UserList users(@Context UriInfo ui);

	@GET
	@Path("users/{id}")
	@Produces({ "application/xml", "application/json" })
	@Description("What do we know about a particular user?")
	UserDesc user(@PathParam("id") String username);

	@POST
	@Path("users")
	@Consumes("application/xml")
	@Description("Create a user.")
	Response useradd(UserDesc userdesc, @NonNull @Context UriInfo ui);

	@PUT
	@Path("users/{id}")
	@Produces({ "application/xml", "application/json" })
	@Consumes("application/xml")
	@Description("Update a user.")
	UserDesc userset(@PathParam("id") String username, UserDesc userdesc);

	@DELETE
	@Path("users/{id}")
	@Produces({ "application/xml", "application/json" })
	@Description("What do we know about a particular user?")
	Response userdel(@PathParam("id") String username);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("users")
	@Description("What users are known to the server?")
	Response optionsUsers();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path("users/{id}")
	@Description("What do we know about a particular user?")
	Response optionsUser(@PathParam("id") String username);

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * The description of what properties are supported by the administration
	 * interface.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "description")
	@XmlType(name = "Description")
	public static class AdminDescription extends VersionedElement {
		public Uri allowNew;
		public Uri logWorkflows;
		public Uri logFaults;
		public Uri usageRecordDumpFile;
		public Uri invokationCount;
		public Uri runCount;
		public Uri registryHost;
		public Uri registryPort;
		public Uri runLimit;
		public Uri defaultLifetime;
		public Uri currentRuns;
		public Uri javaBinary;
		public Uri extraArguments;
		public Uri serverWorkerJar;
		public Uri serverForkerJar;
		public Uri executeWorkflowScript;
		public Uri registrationWaitSeconds;
		public Uri registrationPollMillis;
		public Uri runasPasswordFile;
		public Uri startupTime;
		public Uri lastExitCode;
		public Uri factoryProcessMapping;
		public Uri usageRecords;
		public Uri users;
		public Uri operatingLimit;
		public Uri operatingCount;

		public AdminDescription() {
		}

		public AdminDescription(UriInfo ui) {
			allowNew = new Uri(ui, "allowNew");
			logWorkflows = new Uri(ui, "logWorkflows");
			logFaults = new Uri(ui, "logFaults");
			usageRecordDumpFile = new Uri(ui, "usageRecordDumpFile");
			invokationCount = new Uri(ui, "invokationCount");
			runCount = new Uri(ui, "runCount");
			registryHost = new Uri(ui, "registryHost");
			registryPort = new Uri(ui, "registryPort");
			runLimit = new Uri(ui, "runLimit");
			defaultLifetime = new Uri(ui, "defaultLifetime");
			currentRuns = new Uri(ui, "currentRuns");
			javaBinary = new Uri(ui, "javaBinary");
			extraArguments = new Uri(ui, "extraArguments");
			serverWorkerJar = new Uri(ui, "serverWorkerJar");
			serverForkerJar = new Uri(ui, "serverForkerJar");
			executeWorkflowScript = new Uri(ui, "executeWorkflowScript");
			registrationWaitSeconds = new Uri(ui, "registrationWaitSeconds");
			registrationPollMillis = new Uri(ui, "registrationPollMillis");
			runasPasswordFile = new Uri(ui, "runasPasswordFile");
			startupTime = new Uri(ui, "startupTime");
			lastExitCode = new Uri(ui, "lastExitCode");
			factoryProcessMapping = new Uri(ui, "factoryProcessMapping");
			usageRecords = new Uri(ui, "usageRecords");
			users = new Uri(ui, "users");
			operatingLimit = new Uri(ui, "operatingLimit");
			operatingCount = new Uri(ui, "operatingCount");
		}
	}

	/**
	 * A list of strings, as XML.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "stringList")
	@XmlType(name = "StringList")
	public static class StringList {
		@XmlElement
		public List<String> string = new ArrayList<String>();
	}

	/**
	 * A list of users, as XML.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "userList")
	@XmlType(name = "UserList")
	public static class UserList {
		@XmlElement
		public List<URI> user = new ArrayList<URI>();
	}

	@XmlRootElement(name = "userDesc")
	@XmlType(name = "UserDesc")
	public static class UserDesc {
		@XmlElement
		public String username;
		@XmlElement
		public String password;
		@XmlElement
		public String localUserId;
		@XmlElement
		public Boolean enabled;
		@XmlElement
		public Boolean admin;
	}

	/**
	 * A list of usage records, as XML.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "usageRecordList")
	@XmlType(name = "UsageRecords")
	public static class URList {
		@XmlElement
		public List<JobUsageRecord> usageRecord;
	}
}
