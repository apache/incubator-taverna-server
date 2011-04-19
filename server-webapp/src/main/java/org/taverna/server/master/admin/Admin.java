package org.taverna.server.master.admin;

import static org.taverna.server.master.common.Roles.ADMIN;

import java.util.List;

import javax.annotation.security.RolesAllowed;
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

@RolesAllowed(ADMIN)
@Description("Administration interface for Taverna Server.")
public interface Admin {
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json" })
	@NonNull
	AdminDescription getDescription(@Context UriInfo ui);

	@Path("allowNew")
	BoolProperty allowNew();
	@Path("logWorkflows")
	BoolProperty logWorkflows();
	@Path("logFaults")
	BoolProperty logFaults();
	@Path("usageRecordDumpFile")
	StringProperty urFile();
	@GET
	@Path("invokationCount")
	@Produces("text/plain")
	int invokeCount();
	@GET
	@Path("runCount")
	@Produces("text/plain")
	int runCount();

	@Path("registryHost")
	StringProperty registryHost();
	@Path("registryPort")
	IntegerProperty registryPort();
	@Path("runLimit")
	IntegerProperty runLimit();
	@Path("defaultLifetime")
	IntegerProperty defaultLifetime();
	@GET
	@Path("currentRuns")
	@Produces({ "application/xml", "application/json" })
	StringList currentRuns();

	@Path("javaBinary")
	StringProperty javaBinary();
	@Path("extraArguments")
	StringListProperty extraArguments();
	@Path("serverWorkerJar")
	StringProperty serverWorkerJar();
	@Path("executeWorkflowScript")
	StringProperty executeWorkflowScript();
	@Path("registrationWaitSeconds")
	IntegerProperty registrationWaitSeconds();
	@Path("registrationPollMillis")
	IntegerProperty registrationPollMillis();
	@Path("runasPasswordFile")
	StringProperty runasPasswordFile();
	@Path("serverForkerJar")
	StringProperty serverForkerJar();
	@GET
	@Path("startupTime")
	@Produces("text/plain")
	int startupTime();
	@GET
	@Path("lastExitCode")
	@Produces("text/plain")
	Integer lastExitCode();
	@GET
	@Path("factoryProcessMapping")
	@Produces({ "application/xml", "application/json" })
	StringList factoryProcessMapping();

	@GET
	@Path("usageRecords")
	@Produces("application/xml")
	URList usageRecords();

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

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

		public AdminDescription(){}
		public AdminDescription(UriInfo ui){
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

	@XmlRootElement(name="stringList")
	@XmlType(name="StringList")
	public static class StringList {
		@XmlElement
		public List<String> string;
	}

	@XmlRootElement(name="usageRecordList")
	@XmlType(name="UsageRecords")
	public static class URList {
		@XmlElement
		public List<JobUsageRecord> usageRecord;
	}
}
