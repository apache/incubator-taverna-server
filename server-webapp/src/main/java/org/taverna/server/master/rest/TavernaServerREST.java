package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Namespaces.XLINK;
import static org.taverna.server.master.common.Roles.USER;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.ext.Description;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.soap.TavernaServerSOAP;

/**
 * The REST service interface to Taverna 2 Server release 2.
 * 
 * @author Donal Fellows
 * @see TavernaServerSOAP
 */
@RolesAllowed(USER)
@Description("This is REST service interface to Taverna 2 Server release 2")
public interface TavernaServerREST {
	// MASTER API

	/**
	 * Produces the description of the service.
	 * 
	 * @param ui
	 *            About the URI being accessed.
	 * @return The description.
	 */
	@GET
	@Produces({ "application/xml", "application/json" })
	@Description("Produces the description of the service.")
	ServerDescription describeService(@Context UriInfo ui);

	/**
	 * Produces a description of the list of runs.
	 * 
	 * @param ui
	 *            About the URI being accessed.
	 * @return A description of the list of runs that are available.
	 */
	@GET
	@Path("runs")
	@Produces({ "application/xml", "application/json" })
	@Description("Produces a list of all runs visible to the user.")
	RunList listUsersRuns(@Context UriInfo ui);

	/**
	 * Accepts (or not) a request to create a new run executing the given
	 * workflow.
	 * 
	 * @param workflow
	 *            The workflow document to execute.
	 * @param ui
	 *            About the URI being accessed.
	 * @return A response to the POST describing what was created.
	 * @throws NoUpdateException
	 *             If the POST failed.
	 */
	@POST
	@Path("runs")
	@Consumes("application/xml")
	@Description("Accepts (or not) a request to create a new run executing the given workflow.")
	Response submitWorkflow(Workflow workflow, @Context UriInfo ui)
			throws NoUpdateException;

	/**
	 * @return A description of the policies supported by this server.
	 */
	@Path("policy")
	@Description("The policies supported by this server.")
	PolicyView getPolicyDescription();

	/**
	 * Get a particular named run resource.
	 * 
	 * @param runName
	 *            The name of the run.
	 * @return A RESTful delegate for the run.
	 * @throws UnknownRunException
	 *             If the run handle is unknown to the current user.
	 */
	@Path("runs/{runName}")
	@Description("Get a particular named run resource to dispatch to.")
	TavernaServerRunREST getRunResource(@PathParam("runName") String runName)
			throws UnknownRunException;

	/**
	 * Helper class for describing the server's user-facing management API via
	 * JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class ServerDescription extends VersionedElement {
		/**
		 * References to the runs (known about by the current user) in this
		 * server.
		 */
		public PointingRunList runs;
		/**
		 * Reference to the policy description part of this server.
		 */
		public Uri policy;
		/**
		 * Where to go to make queries on the provenance database. Not yet
		 * supported, so not handled by JAXB.
		 */
		@XmlTransient
		public Uri database;

		/** Make a blank server description. */
		public ServerDescription() {
		}

		/**
		 * Make a description of the server.
		 * 
		 * @param ws
		 *            The mapping of run names to runs.
		 * @param ui
		 *            The factory for URIs.
		 */
		public ServerDescription(Map<String, TavernaRun> ws, UriInfo ui) {
			super(true);
			runs = new PointingRunList(ws, ui.getAbsolutePathBuilder().path(
					"runs/{uuid}"), ui.getAbsolutePathBuilder().path("runs")
					.build());
			policy = new Uri(ui, "policy");
			// database = new Uri(ui, "database");
			// TODO make the database point to something real
		}
	}

	/**
	 * How to discover the publicly-visible policies supported by this server.
	 * 
	 * @author Donal Fellows
	 */
	@RolesAllowed(USER)
	public interface PolicyView {
		@GET
		@Path("/")
		@Produces({ "application/xml", "application/json" })
		@Description("Describe the parts of this policy.")
		public PolicyDescription getDescription(@Context UriInfo ui);

		/**
		 * Gets the maximum number of simultaneous runs that the user may
		 * create. The <i>actual</i> number they can create may be lower than
		 * this. If this number is lower than the number they currently have,
		 * they will be unable to create any runs at all.
		 * 
		 * @return The maximum number of runs.
		 */
		@GET
		@Path("runLimit")
		@Produces("text/plain")
		@Description("Gets the maximum number of simultaneous runs that the user may create.")
		public int getMaxSimultaneousRuns();

		/**
		 * Gets the list of permitted workflows. Any workflow may be submitted
		 * if the list is empty, otherwise it must be one of the workflows on
		 * this list.
		 * 
		 * @return The list of workflow documents.
		 */
		@GET
		@Path("permittedWorkflows")
		@Produces({ "application/xml", "application/json" })
		@Description("Gets the list of permitted workflows.")
		public PermittedWorkflows getPermittedWorkflows();

		/**
		 * Gets the list of permitted event listener types. All event listeners
		 * must be of a type described on this list.
		 * 
		 * @return The types of event listeners allowed.
		 */
		@GET
		@Path("permittedListenerTypes")
		@Produces({ "application/xml", "application/json" })
		@Description("Gets the list of permitted event listener types.")
		public PermittedListeners getPermittedListeners();

		/**
		 * A description of the parts of a server policy.
		 * 
		 * @author Donal Fellows
		 */
		@XmlRootElement
		@XmlType(name = "")
		public static class PolicyDescription extends VersionedElement {
			/**
			 * Where to go to find out about the maximum number of runs.
			 */
			public Uri runLimit;
			/**
			 * Where to go to find out about what workflows are allowed.
			 */
			public Uri permittedWorkflows;
			/**
			 * Where to go to find out about what listeners are allowed.
			 */
			public Uri permittedListenerTypes;

			/** Make a blank server description. */
			public PolicyDescription() {
			}

			/** Make a server description. */
			public PolicyDescription(UriInfo ui) {
				super(true);
				runLimit = new Uri(ui, "runLimit");
				permittedWorkflows = new Uri(ui, "permittedWorkflows");
				permittedListenerTypes = new Uri(ui, "permittedListenerTypes");
			}
		}
	}

	/**
	 * Helper class for describing the workflows that are allowed via JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class PermittedWorkflows {
		/** The workflows that are permitted. */
		@XmlElement
		public List<Workflow> workflow;

		/**
		 * Make an empty list of permitted workflows.
		 */
		public PermittedWorkflows() {
			workflow = new ArrayList<Workflow>();
		}

		/**
		 * Make a list of permitted workflows.
		 * 
		 * @param permitted
		 */
		public PermittedWorkflows(List<Workflow> permitted) {
			if (permitted == null)
				workflow = new ArrayList<Workflow>();
			else
				workflow = new ArrayList<Workflow>(permitted);
		}
	}

	/**
	 * Helper class for describing the listener types that are allowed via JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class PermittedListeners {
		/** The listener types that are permitted. */
		@XmlElement
		public List<String> type;

		/**
		 * Make an empty list of permitted listener types.
		 */
		public PermittedListeners() {
			type = new ArrayList<String>();
		}

		/**
		 * Make a list of permitted listener types.
		 * 
		 * @param listenerTypes
		 */
		public PermittedListeners(List<String> listenerTypes) {
			type = listenerTypes;
		}
	}

	/**
	 * Helper class for describing the workflow runs.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class RunList {
		/** The references to the workflow runs. */
		@XmlElement
		public List<RunReference> run;

		/**
		 * Make an empty list of run references.
		 */
		public RunList() {
			run = new ArrayList<RunReference>();
		}

		/**
		 * Make a list of references to workflow runs.
		 */
		public RunList(Map<String, TavernaRun> runs, UriBuilder ub) {
			run = new ArrayList<RunReference>(runs.size());
			for (String name : runs.keySet())
				run.add(new RunReference(name, ub));
		}
	}

	@XmlType(name = "RunList")
	public static class PointingRunList {
		/** The reference to the real list of runs. */
		@XmlAttribute(name = "href", namespace = XLINK)
		public URI href;
		/** The references to the workflow runs. */
		@XmlElement
		public List<Uri> run;

		/**
		 * Make an empty list of run references.
		 */
		public PointingRunList() {
			run = new ArrayList<Uri>();
		}

		/**
		 * Make a list of references to workflow runs.
		 */
		public PointingRunList(Map<String, TavernaRun> runs, UriBuilder ub,
				URI uri) {
			run = new ArrayList<Uri>(runs.size());
			for (String name : runs.keySet())
				run.add(new Uri(ub.build(name)));
			href = uri;
		}
	}
}
