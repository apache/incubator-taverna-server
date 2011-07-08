/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.admin;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.ext.Description;
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

	/**
	 * The property for whether to allow new workflow runs to be created.
	 * 
	 * @return An updatable property.
	 */
	@Path("allowNew")
	@Description("Whether to allow new workflow runs to be created.")
	BoolProperty allowNew();

	/**
	 * The property for whether to log the workflows submitted.
	 * 
	 * @return An updatable property.
	 */
	@Path("logWorkflows")
	@Description("Whether to log the workflows submitted.")
	BoolProperty logWorkflows();

	/**
	 * The property for whether to log the user-directed faults.
	 * 
	 * @return An updatable property.
	 */
	@Path("logFaults")
	@Description("Whether to log the user-directed faults.")
	BoolProperty logFaults();

	/**
	 * The property for what file to dump usage records to.
	 * 
	 * @return An updatable property.
	 */
	@Path("usageRecordDumpFile")
	@Description("What file to dump usage records to.")
	StringProperty urFile();

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

	/**
	 * The property for the location of the RMI registry.
	 * 
	 * @return An updatable property.
	 */
	@Path("registryHost")
	@Description("Where is the RMI registry?")
	StringProperty registryHost();

	/**
	 * The property for the port of the RMI registry.
	 * 
	 * @return An updatable property.
	 */
	@Path("registryPort")
	@Description("On what port is the RMI registry?")
	IntegerProperty registryPort();

	/**
	 * The property for the maximum number of simultaneous runs.
	 * 
	 * @return An updatable property.
	 */
	@Path("runLimit")
	@Description("What is the maximum number of simultaneous runs?")
	IntegerProperty runLimit();

	/**
	 * The property for the default lifetime of workflow runs.
	 * 
	 * @return An updatable property.
	 */
	@Path("defaultLifetime")
	@Description("What is the default lifetime of workflow runs, in seconds?")
	IntegerProperty defaultLifetime();

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

	/**
	 * The property for the Java binary to be used for execution of
	 * subprocesses.
	 * 
	 * @return An updatable property.
	 */
	@Path("javaBinary")
	@Description("Which Java binary should be used for execution of subprocesses?")
	StringProperty javaBinary();

	/**
	 * The property for the extra arguments to be supplied to Java subprocesses.
	 * 
	 * @return An updatable property.
	 */
	@Path("extraArguments")
	@Description("What extra arguments should be supplied to Java subprocesses?")
	StringListProperty extraArguments();

	/**
	 * The property for the full pathname of the worker JAR file.
	 * 
	 * @return An updatable property.
	 */
	@Path("serverWorkerJar")
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	StringProperty serverWorkerJar();

	/**
	 * The property for the full pathname of the executeWorkflow.sh file.
	 * 
	 * @return An updatable property.
	 */
	@Path("executeWorkflowScript")
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	StringProperty executeWorkflowScript();

	/**
	 * The property for the total duration of time to wait for the start of the
	 * forker process.
	 * 
	 * @return An updatable property.
	 */
	@Path("registrationWaitSeconds")
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	IntegerProperty registrationWaitSeconds();

	/**
	 * The property for the interval between checks for registration of the
	 * forker process.
	 * 
	 * @return An updatable property.
	 */
	@Path("registrationPollMillis")
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	IntegerProperty registrationPollMillis();

	/**
	 * The property for the full pathname of the file containing the
	 * impersonation credentials for the forker process.
	 * 
	 * @return An updatable property.
	 */
	@Path("runasPasswordFile")
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	StringProperty runasPasswordFile();

	/**
	 * The property for the full pathname of the forker's JAR.
	 * 
	 * @return An updatable property.
	 */
	@Path("serverForkerJar")
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	StringProperty serverForkerJar();

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

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	/**
	 * A boolean REST property.
	 * 
	 * @author Donal Fellows
	 */
	public interface BoolProperty {
		@GET
		@Path("/")
		@Produces("text/plain")
		@NonNull
		public boolean get();

		@PUT
		@Path("/")
		@Consumes("text/plain")
		@Produces("text/plain")
		@NonNull
		public boolean set(@NonNull boolean newValue);
	}

	/**
	 * A string REST property.
	 * 
	 * @author Donal Fellows
	 */
	public interface StringProperty {
		@GET
		@Path("/")
		@Produces("text/plain")
		@NonNull
		public String get();

		@PUT
		@Path("/")
		@Consumes("text/plain")
		@Produces("text/plain")
		@NonNull
		public String set(@NonNull String newValue);
	}

	/**
	 * A string-list REST property.
	 * 
	 * @author Donal Fellows
	 */
	public interface StringListProperty {
		@GET
		@Path("/")
		@Produces({ "application/xml", "application/json" })
		@NonNull
		public StringList get();

		@PUT
		@Path("/")
		@Consumes({ "application/xml", "application/json" })
		@Produces({ "application/xml", "application/json" })
		@NonNull
		public StringList set(@NonNull StringList newValue);
	}

	/**
	 * An integer REST property.
	 * 
	 * @author Donal Fellows
	 */
	public interface IntegerProperty {
		@GET
		@Path("/")
		@Produces("text/plain")
		@NonNull
		public int get();

		@PUT
		@Path("/")
		@Consumes("text/plain")
		@Produces("text/plain")
		@NonNull
		public int set(@NonNull int newValue);
	}

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
		public List<String> string;
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
