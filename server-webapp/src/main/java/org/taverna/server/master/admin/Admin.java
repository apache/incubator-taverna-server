/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.admin;

import static org.taverna.server.master.admin.Paths.ALLOW_NEW;
import static org.taverna.server.master.admin.Paths.ARGS;
import static org.taverna.server.master.admin.Paths.EXEC_WF;
import static org.taverna.server.master.admin.Paths.EXITCODE;
import static org.taverna.server.master.admin.Paths.FACTORIES;
import static org.taverna.server.master.admin.Paths.GEN_PROV;
import static org.taverna.server.master.admin.Paths.INVOKES;
import static org.taverna.server.master.admin.Paths.JAR_FORKER;
import static org.taverna.server.master.admin.Paths.JAR_WORKER;
import static org.taverna.server.master.admin.Paths.JAVA;
import static org.taverna.server.master.admin.Paths.LIFE;
import static org.taverna.server.master.admin.Paths.LOG_EXN;
import static org.taverna.server.master.admin.Paths.LOG_WFS;
import static org.taverna.server.master.admin.Paths.OPERATING;
import static org.taverna.server.master.admin.Paths.OP_LIMIT;
import static org.taverna.server.master.admin.Paths.PASSFILE;
import static org.taverna.server.master.admin.Paths.PERM_WF;
import static org.taverna.server.master.admin.Paths.REG_HOST;
import static org.taverna.server.master.admin.Paths.REG_JAR;
import static org.taverna.server.master.admin.Paths.REG_POLL;
import static org.taverna.server.master.admin.Paths.REG_PORT;
import static org.taverna.server.master.admin.Paths.REG_WAIT;
import static org.taverna.server.master.admin.Paths.ROOT;
import static org.taverna.server.master.admin.Paths.RUNS;
import static org.taverna.server.master.admin.Paths.RUN_LIMIT;
import static org.taverna.server.master.admin.Paths.STARTUP;
import static org.taverna.server.master.admin.Paths.TOTAL_RUNS;
import static org.taverna.server.master.admin.Paths.URS;
import static org.taverna.server.master.admin.Paths.UR_FILE;
import static org.taverna.server.master.admin.Paths.USER;
import static org.taverna.server.master.admin.Paths.USERS;
import static org.taverna.server.master.admin.Types.JSON;
import static org.taverna.server.master.admin.Types.PLAIN;
import static org.taverna.server.master.admin.Types.XML;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
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
	@Path(ROOT)
	@Produces("text/html")
	@Nonnull
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
	 * Get a description of the administration interface.
	 * 
	 * @param ui
	 *            What URI was used to access this resource?
	 * @return The description document.
	 */
	@GET
	@Path(ROOT)
	@Produces({ XML, JSON })
	@Nonnull
	AdminDescription getDescription(@Context UriInfo ui);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(ROOT)
	Response optionsRoot();

	/**
	 * Get whether to allow new workflow runs to be created.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(ALLOW_NEW)
	@Produces(PLAIN)
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
	@Path(ALLOW_NEW)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Whether to allow new workflow runs to be created.")
	boolean setAllowNew(boolean newValue);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(ALLOW_NEW)
	@Description("Whether to allow new workflow runs to be created.")
	Response optionsAllowNew();

	/**
	 * Get whether to log the workflows submitted.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(LOG_WFS)
	@Produces(PLAIN)
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
	@Path(LOG_WFS)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Whether to log the workflows submitted.")
	boolean setLogWorkflows(boolean logWorkflows);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(LOG_WFS)
	@Description("Whether to log the workflows submitted.")
	Response optionsLogWorkflows();

	/**
	 * Get whether to log the user-directed faults.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(LOG_EXN)
	@Produces(PLAIN)
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
	@Path(LOG_EXN)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Whether to log the user-directed faults.")
	boolean setLogFaults(boolean logFaults);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(LOG_EXN)
	@Description("Whether to log the user-directed faults.")
	Response optionsLogFaults();

	/**
	 * Get what file to dump usage records to.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(UR_FILE)
	@Produces(PLAIN)
	@Description("What file to dump usage records to.")
	@Nonnull
	String getURFile();

	/**
	 * Set what file to dump usage records to.
	 * 
	 * @param urFile
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(UR_FILE)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What file to dump usage records to.")
	@Nonnull
	String setURFile(@Nonnull String urFile);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(UR_FILE)
	@Description("What file to dump usage records to.")
	Response optionsURFile();

	/**
	 * The property for the number of times the service methods have been
	 * invoked.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(INVOKES)
	@Produces(PLAIN)
	@Description("How many times have the service methods been invoked.")
	int invokeCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(INVOKES)
	@Description("How many times have the service methods been invoked.")
	Response optionsInvokationCount();

	/**
	 * The property for the number of runs that are currently in existence.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(TOTAL_RUNS)
	@Produces(PLAIN)
	@Description("How many runs are currently in existence.")
	int runCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(TOTAL_RUNS)
	@Description("How many runs are currently in existence.")
	Response optionsRunCount();

	/**
	 * The property for the number of runs that are currently running.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(OPERATING)
	@Produces(PLAIN)
	@Description("How many runs are currently actually running.")
	int operatingCount();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(OPERATING)
	@Description("How many runs are currently actually running.")
	Response optionsOperatingCount();

	/**
	 * Get the full pathname of the RMI registry's JAR.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(REG_JAR)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's custom RMI registry executable JAR file?")
	@Nonnull
	String getRegistryJar();

	/**
	 * Set the full pathname of the RMI registry's JAR.
	 * 
	 * @param registryJar
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(REG_JAR)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's custom RMI registry executable JAR file?")
	@Nonnull
	String setRegistryJar(@Nonnull String registryJar);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(REG_JAR)
	@Description("What is the full pathname of the server's special custom RMI registry executable JAR file?")
	Response optionsRegistryJar();

	/**
	 * Get the location of the RMI registry.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(REG_HOST)
	@Produces(PLAIN)
	@Description("Where is the RMI registry?")
	@Nonnull
	String getRegistryHost();

	/**
	 * Set the location of the RMI registry.
	 * 
	 * @param registryHost
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(REG_HOST)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Where is the RMI registry?")
	@Nonnull
	String setRegistryHost(@Nonnull String registryHost);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(REG_HOST)
	@Description("Where is the RMI registry?")
	Response optionsRegistryHost();

	/**
	 * Get the port of the RMI registry.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(REG_PORT)
	@Produces(PLAIN)
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
	@Path(REG_PORT)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("On what port is the RMI registry?")
	int setRegistryPort(int registryPort);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(REG_PORT)
	@Description("On what port is the RMI registry?")
	Response optionsRegistryPort();

	/**
	 * Get the maximum number of simultaneous runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(RUN_LIMIT)
	@Produces(PLAIN)
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
	@Path(RUN_LIMIT)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the maximum number of simultaneous runs?")
	int setRunLimit(int runLimit);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(RUN_LIMIT)
	@Description("What is the maximum number of simultaneous runs?")
	Response optionsRunLimit();

	/**
	 * Get the maximum number of simultaneous executing runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(OP_LIMIT)
	@Produces(PLAIN)
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
	@Path(OP_LIMIT)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the maximum number of simultaneous executing runs?")
	int setOperatingLimit(int operatingLimit);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(OP_LIMIT)
	@Description("What is the maximum number of simultaneous executing runs?")
	Response optionsOperatingLimit();

	/**
	 * Get the default lifetime of workflow runs.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(LIFE)
	@Produces(PLAIN)
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
	@Path(LIFE)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the default lifetime of workflow runs, in seconds?")
	int setDefaultLifetime(int defaultLifetime);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(LIFE)
	@Description("What is the default lifetime of workflow runs, in seconds?")
	Response optionsDefaultLifetime();

	/**
	 * The property for the list of IDs of current runs.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(RUNS)
	@Produces({ XML, JSON })
	@Description("List the IDs of all current runs.")
	StringList currentRuns();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(RUNS)
	@Description("List the IDs of all current runs.")
	Response optionsCurrentRuns();

	/**
	 * Get the Java binary to be used for execution of subprocesses.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(JAVA)
	@Produces(PLAIN)
	@Description("Which Java binary should be used for execution of subprocesses?")
	@Nonnull
	String getJavaBinary();

	/**
	 * Set the Java binary to be used for execution of subprocesses.
	 * 
	 * @param javaBinary
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(JAVA)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Which Java binary should be used for execution of subprocesses?")
	@Nonnull
	String setJavaBinary(@Nonnull String javaBinary);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(JAVA)
	@Description("Which Java binary should be used for execution of subprocesses?")
	Response optionsJavaBinary();

	/**
	 * Get the extra arguments to be supplied to Java subprocesses.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(ARGS)
	@Produces({ XML, JSON })
	@Description("What extra arguments should be supplied to Java subprocesses?")
	@Nonnull
	StringList getExtraArguments();

	/**
	 * Set the extra arguments to be supplied to Java subprocesses.
	 * 
	 * @param extraArguments
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(ARGS)
	@Consumes(XML)
	@Produces({ XML, JSON })
	@Description("What extra arguments should be supplied to Java subprocesses?")
	@Nonnull
	StringList setExtraArguments(@Nonnull StringList extraArguments);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(ARGS)
	@Description("What extra arguments should be supplied to Java subprocesses?")
	Response optionsExtraArguments();

	/**
	 * Get the full pathname of the worker JAR file.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(JAR_WORKER)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	@Nonnull
	String getServerWorkerJar();

	/**
	 * Set the full pathname of the worker JAR file.
	 * 
	 * @param serverWorkerJar
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(JAR_WORKER)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	@Nonnull
	String setServerWorkerJar(@Nonnull String serverWorkerJar);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(JAR_WORKER)
	@Description("What is the full pathname of the server's per-user worker executable JAR file?")
	Response optionsServerWorkerJar();

	/**
	 * Get the full pathname of the executeWorkflow.sh file.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(EXEC_WF)
	@Produces(PLAIN)
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	@Nonnull
	String getExecuteWorkflowScript();

	/**
	 * Set the full pathname of the executeWorkflow.sh file.
	 * 
	 * @param executeWorkflowScript
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(EXEC_WF)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	@Nonnull
	String setExecuteWorkflowScript(@Nonnull String executeWorkflowScript);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(EXEC_WF)
	@Description("What is the full pathname of the core Taverna executeWorkflow script?")
	Response optionsExecuteWorkflowScript();

	/**
	 * Get the total duration of time to wait for the start of the forker
	 * process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(REG_WAIT)
	@Produces(PLAIN)
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
	@Path(REG_WAIT)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	int setRegistrationWaitSeconds(int registrationWaitSeconds);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(REG_WAIT)
	@Description("How long in total should the core wait for registration of the \"forker\" process, in seconds.")
	Response optionsRegistrationWaitSeconds();

	/**
	 * Get the interval between checks for registration of the forker process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(REG_POLL)
	@Produces(PLAIN)
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
	@Path(REG_POLL)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	int setRegistrationPollMillis(int registrationPollMillis);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(REG_POLL)
	@Description("What is the interval between checks for registration of the \"forker\" process, in milliseconds.")
	Response optionsRegistrationPollMillis();

	/**
	 * Get the full pathname of the file containing the impersonation
	 * credentials for the forker process.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(PASSFILE)
	@Produces(PLAIN)
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	@Nonnull
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
	@Path(PASSFILE)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	@Nonnull
	String setRunasPasswordFile(@Nonnull String runasPasswordFile);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(PASSFILE)
	@Description("What is the full pathname of the file containing the password used for impersonating other users? (On Unix, this is the password for the deployment user to use \"sudo\".)")
	Response optionsRunasPasswordFile();

	/**
	 * Get the full pathname of the forker's JAR.
	 * 
	 * @return The current setting.
	 */
	@GET
	@Path(JAR_FORKER)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	@Nonnull
	String getServerForkerJar();

	/**
	 * Set the full pathname of the forker's JAR.
	 * 
	 * @param serverForkerJar
	 *            What to set it to.
	 * @return The new setting.
	 */
	@PUT
	@Path(JAR_FORKER)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	@Nonnull
	String setServerForkerJar(@Nonnull String serverForkerJar);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(JAR_FORKER)
	@Description("What is the full pathname of the server's special authorized \"forker\" executable JAR file?")
	Response optionsServerForkerJar();

	/**
	 * The property for the length of time it took to start the forker.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(STARTUP)
	@Produces(PLAIN)
	@Description("How long did it take for the back-end \"forker\" to set itself up, in seconds.")
	int startupTime();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(STARTUP)
	@Description("How long did it take for the back-end \"forker\" to set itself up, in seconds.")
	Response optionsStartupTime();

	/**
	 * The property for the last exit code of the forker process.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(EXITCODE)
	@Produces(PLAIN)
	@Description("What was the last exit code of the \"forker\"? If null, no exit has ever been recorded.")
	Integer lastExitCode();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(EXITCODE)
	@Description("What was the last exit code of the \"forker\"? If null, no exit has ever been recorded.")
	Response optionsLastExitCode();

	/**
	 * The property for the mapping of usernames to factory process handles.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(FACTORIES)
	@Produces({ XML, JSON })
	@Description("What is the mapping of local usernames to factory process RMI IDs?")
	StringList factoryProcessMapping();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(FACTORIES)
	@Description("What is the mapping of local usernames to factory process RMI IDs?")
	Response optionsFactoryProcessMapping();

	/**
	 * The property for the list of usage records collected.
	 * 
	 * @return The property value (read-only).
	 */
	@GET
	@Path(URS)
	@Produces(XML)
	@Description("What is the list of usage records that have been collected?")
	URList usageRecords();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(URS)
	@Description("What is the list of usage records that have been collected?")
	Response optionsUsageRecords();

	/**
	 * What are the current list of workflow URIs that may be started? Empty
	 * means allow any, including user-supplied workflows.
	 * 
	 * @return List of URIs, encoded as strings.
	 */
	@GET
	@Path(PERM_WF)
	@Produces({ XML, JSON })
	@Description("What are the current list of workflow URIs that may be started? Empty means allow any, including user-supplied workflows.")
	StringList getPermittedWorkflowURIs();

	/** Do we turn on the generate provenance option by default? */
	@GET
	@Path(GEN_PROV)
	@Produces(PLAIN)
	@Description("Do we turn on the generate provenance option by default? (boolean)")
	String getGenerateProvenance();

	/** Do we turn on the generate provenance option by default? */
	@PUT
	@Path(GEN_PROV)
	@Consumes(PLAIN)
	@Produces(PLAIN)
	@Description("Do we turn on the generate provenance option by default? (boolean)")
	String setGenerateProvenance(String newValue);

	/** Do we turn on the generate provenance option by default? */
	@OPTIONS
	@Path(GEN_PROV)
	@Description("Do we turn on the generate provenance option by default? (boolean)")
	Response optionsGenerateProvenance();

	/**
	 * What are the current list of workflow URIs that may be started? Empty
	 * means allow any, including user-supplied workflows.
	 * 
	 * @param permitted
	 *            List of URIs, encoded as strings.
	 * @return List of URIs, encoded as strings.
	 */
	@PUT
	@Path(PERM_WF)
	@Consumes(XML)
	@Produces({ XML, JSON })
	@Description("What are the current list of workflow URIs that may be started? Empty means allow any, including user-supplied workflows.")
	StringList setPermittedWorkflowURIs(@Nonnull StringList permitted);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(PERM_WF)
	@Description("What are the current list of workflow URIs that may be started? Empty means allow any, including user-supplied workflows.")
	Response optionsPermittedWorkflowURIs();

	@GET
	@Path(USERS)
	@Produces({ XML, JSON })
	@Description("What users are known to the server?")
	UserList users(@Context UriInfo ui);

	@GET
	@Path(USER)
	@Produces({ XML, JSON })
	@Description("What do we know about a particular user?")
	UserDesc user(@PathParam("id") String username);

	@POST
	@Path(USERS)
	@Consumes(XML)
	@Description("Create a user.")
	Response useradd(UserDesc userdesc, @Nonnull @Context UriInfo ui);

	@PUT
	@Path(USER)
	@Produces({ XML, JSON })
	@Consumes(XML)
	@Description("Update a user.")
	UserDesc userset(@PathParam("id") String username, UserDesc userdesc);

	@DELETE
	@Path(USER)
	@Produces({ XML, JSON })
	@Description("What do we know about a particular user?")
	Response userdel(@PathParam("id") String username);

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(USERS)
	@Description("What users are known to the server?")
	Response optionsUsers();

	/** What HTTP methods may we use? */
	@OPTIONS
	@Path(USER)
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
		public Uri permittedWorkflowURIs;
		public Uri generateProvenance;

		public AdminDescription() {
		}

		public AdminDescription(UriInfo ui) {
			allowNew = new Uri(ui, ALLOW_NEW);
			logWorkflows = new Uri(ui, LOG_WFS);
			logFaults = new Uri(ui, LOG_EXN);
			usageRecordDumpFile = new Uri(ui, UR_FILE);
			invokationCount = new Uri(ui, INVOKES);
			runCount = new Uri(ui, TOTAL_RUNS);
			registryHost = new Uri(ui, REG_HOST);
			registryPort = new Uri(ui, REG_PORT);
			runLimit = new Uri(ui, RUN_LIMIT);
			defaultLifetime = new Uri(ui, LIFE);
			currentRuns = new Uri(ui, RUNS);
			javaBinary = new Uri(ui, JAVA);
			extraArguments = new Uri(ui, ARGS);
			serverWorkerJar = new Uri(ui, JAR_WORKER);
			serverForkerJar = new Uri(ui, JAR_FORKER);
			executeWorkflowScript = new Uri(ui, EXEC_WF);
			registrationWaitSeconds = new Uri(ui, REG_WAIT);
			registrationPollMillis = new Uri(ui, REG_POLL);
			runasPasswordFile = new Uri(ui, PASSFILE);
			startupTime = new Uri(ui, STARTUP);
			lastExitCode = new Uri(ui, EXITCODE);
			factoryProcessMapping = new Uri(ui, FACTORIES);
			usageRecords = new Uri(ui, URS);
			users = new Uri(ui, USERS);
			operatingLimit = new Uri(ui, OP_LIMIT);
			operatingCount = new Uri(ui, OPERATING);
			permittedWorkflowURIs = new Uri(ui, PERM_WF);
			generateProvenance = new Uri(ui, GEN_PROV);
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
		public List<String> string = new ArrayList<>();
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
		public List<URI> user = new ArrayList<>();
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

interface Paths {
	static final String ROOT = "/";
	static final String ALLOW_NEW = "allowNew";
	static final String LOG_WFS = "logWorkflows";
	static final String LOG_EXN = "logFaults";
	static final String UR_FILE = "usageRecordDumpFile";
	static final String INVOKES = "invokationCount";
	static final String TOTAL_RUNS = "runCount";
	static final String OPERATING = "operatingCount";
	static final String REG_HOST = "registryHost";
	static final String REG_PORT = "registryPort";
	static final String REG_JAR = "registryJar";
	static final String RUN_LIMIT = "runLimit";
	static final String OP_LIMIT = "operatingLimit";
	static final String LIFE = "defaultLifetime";
	static final String RUNS = "currentRuns";
	static final String JAVA = "javaBinary";
	static final String ARGS = "extraArguments";
	static final String JAR_WORKER = "serverWorkerJar";
	static final String JAR_FORKER = "serverForkerJar";
	static final String EXEC_WF = "executeWorkflowScript";
	static final String REG_WAIT = "registrationWaitSeconds";
	static final String REG_POLL = "registrationPollMillis";
	static final String PASSFILE = "runasPasswordFile";
	static final String STARTUP = "startupTime";
	static final String EXITCODE = "lastExitCode";
	static final String FACTORIES = "factoryProcessMapping";
	static final String URS = "usageRecords";
	static final String PERM_WF = "permittedWorkflowURIs";
	static final String GEN_PROV = "generateProvenance";
	static final String USERS = "users";
	static final String USER = USERS + "/{id}";
}

interface Types {
	static final String PLAIN = "text/plain";
	static final String XML = "application/xml";
	static final String JSON = "application/json";
}
