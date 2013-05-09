/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.notification.atom.AbstractEvent;
import org.taverna.server.master.soap.TavernaServerSOAP;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The REST service interface to Taverna 2.4 Server Release 2.
 * 
 * @author Donal Fellows
 * @see TavernaServerSOAP
 */
@RolesAllowed(USER)
@Description("This is REST service interface to Taverna 2.4 Server Release 2.")
public interface TavernaServerREST {
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
	@NonNull
	ServerDescription describeService(@NonNull @Context UriInfo ui);

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Description("Produces the description of the service.")
	Response serviceOptions();

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
	@RolesAllowed(USER)
	@Description("Produces a list of all runs visible to the user.")
	@NonNull
	RunList listUsersRuns(@NonNull @Context UriInfo ui);

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
	@Consumes({ T2FLOW, "application/xml" })
	@RolesAllowed(USER)
	@Description("Accepts (or not) a request to create a new run executing the given workflow.")
	@NonNull
	Response submitWorkflow(@NonNull Workflow workflow,
			@NonNull @Context UriInfo ui) throws NoUpdateException;

	/**
	 * Accepts (or not) a request to create a new run executing the workflow at
	 * the given location.
	 * 
	 * @param referenceList
	 *            The URI to workflow document to execute.
	 * @param ui
	 *            About the URI being POSTed to.
	 * @return A response to the POST describing what was created.
	 * @throws NoUpdateException
	 *             If the POST failed.
	 * @throws NoCreateException
	 *             If the workflow couldn't be read into the server or the
	 *             engine rejects it.
	 */
	@POST
	@Path("runs")
	@Consumes("text/uri-list")
	@RolesAllowed(USER)
	@Description("Accepts a URL to a workflow to download and run. The URL must be hosted on a publicly-accessible service.")
	@NonNull
	Response submitWorkflowByURL(@NonNull List<URI> referenceList,
			@NonNull @Context UriInfo ui) throws NoCreateException,
			NoUpdateException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("runs")
	@Description("Produces the description of the operations on the collection of runs.")
	Response runsOptions();

	/**
	 * @return A description of the policies supported by this server.
	 */
	@Path("policy")
	@Description("The policies supported by this server.")
	@NonNull
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
	@RolesAllowed(USER)
	@Description("Get a particular named run resource to dispatch to.")
	@NonNull
	TavernaServerRunREST getRunResource(
			@NonNull @PathParam("runName") String runName)
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
		 * References to the collection of runs (known about by the current
		 * user) in this server.
		 */
		public Uri runs;
		/**
		 * Reference to the policy description part of this server.
		 */
		public Uri policy;
		/**
		 * Reference to the Atom event feed produced by this server.
		 */
		public Uri feed;
		/**
		 * Reference to the interaction feed for this server.
		 */
		public Uri interactionFeed;
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
		 * @param ui
		 *            The factory for URIs.
		 */
		public ServerDescription(UriInfo ui, String interactionFeed) {
			super(true);
			String base = ui.getBaseUri().toString();
			runs = new Uri(ui, "runs");
			policy = new Uri(ui, false, "policy");
			feed = new Uri(java.net.URI.create(base.replaceFirst("/rest$",
					"/feed")));
			if (interactionFeed != null && !interactionFeed.isEmpty())
				this.interactionFeed = new Uri(
						java.net.URI.create(interactionFeed));
			// database = new Uri(ui, "database");
			// TODO TAVSERV-69: Make the database point to something real
		}
	}

	/**
	 * How to discover the publicly-visible policies supported by this server.
	 * 
	 * @author Donal Fellows
	 */
	public interface PolicyView {
		/**
		 * Describe the URIs in this view of the server's policies.
		 * 
		 * @param ui
		 *            About the URI used to retrieve the description.
		 * @return The description, which may be serialised as XML or JSON.
		 */
		@GET
		@Path("/")
		@Produces({ "application/xml", "application/json" })
		@Description("Describe the parts of this policy.")
		@NonNull
		public PolicyDescription getDescription(@NonNull @Context UriInfo ui);

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
		@RolesAllowed(USER)
		@Description("Gets the maximum number of simultaneous runs that the user may create.")
		@NonNull
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
		@RolesAllowed(USER)
		@Description("Gets the list of permitted workflows.")
		@NonNull
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
		@RolesAllowed(USER)
		@Description("Gets the list of permitted event listener types.")
		@NonNull
		public PermittedListeners getPermittedListeners();

		/**
		 * Gets the list of supported, enabled notification fabrics. Each
		 * corresponds (approximately) to a protocol, e.g., email.
		 * 
		 * @return List of notifier names; each is the scheme of a notification
		 *         destination URI.
		 */
		@GET
		@Path("enabledNotificationFabrics")
		@Produces({ "application/xml", "application/json" })
		@RolesAllowed(USER)
		@Description("Gets the list of supported, enabled notification fabrics. Each corresponds (approximately) to a protocol, e.g., email.")
		@NonNull
		public EnabledNotificationFabrics getEnabledNotifiers();

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
			/**
			 * How notifications may be sent.
			 */
			public Uri enabledNotificationFabrics;

			/** Make a blank server description. */
			public PolicyDescription() {
			}

			/**
			 * Make a server description.
			 * 
			 * @param ui
			 *            About the URI used to access this description.
			 */
			public PolicyDescription(UriInfo ui) {
				super(true);
				runLimit = new Uri(ui, false, "runLimit");
				permittedWorkflows = new Uri(ui, false, "permittedWorkflows");
				permittedListenerTypes = new Uri(ui, false,
						"permittedListenerTypes");
				this.enabledNotificationFabrics = new Uri(ui, false,
						"enabledNotificationFabrics");
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
		 * 
		 * @param runs
		 *            The mapping of runs to describe.
		 * @param ub
		 *            How to construct URIs to the runs. Must have already been
		 *            secured as it needs to have its pattern applied.
		 */
		public RunList(Map<String, TavernaRun> runs, UriBuilder ub) {
			run = new ArrayList<RunReference>(runs.size());
			for (String name : runs.keySet())
				run.add(new RunReference(name, ub));
		}
	}

	/**
	 * Helper class for describing the listener types that are allowed via JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class EnabledNotificationFabrics {
		/** The notification fabrics that are enabled. */
		@XmlElement
		public List<String> notifier;

		/**
		 * Make an empty list of enabled notifiers.
		 */
		public EnabledNotificationFabrics() {
			notifier = new ArrayList<String>();
		}

		/**
		 * Make a list of enabled notifiers.
		 * 
		 * @param enabledNodifiers
		 */
		public EnabledNotificationFabrics(List<String> enabledNodifiers) {
			notifier = enabledNodifiers;
		}
	}

	/**
	 * The interface exposed by the Atom feed of events.
	 * 
	 * @author Donal Fellows
	 */
	@RolesAllowed(USER)
	public interface EventFeed {
		/**
		 * @return the feed of events for the current user.
		 */
		@GET
		@Path("/")
		@Produces({ "application/xml", "application/json",
				"application/atom+xml;type=feed" })
		@Description("Get an Atom feed for the user's events.")
		@NonNull
		Events getFeed();

		/**
		 * @param id
		 *            The identifier for a particular event.
		 * @return the details about the given event.
		 */
		@GET
		@Path("{id}")
		@Produces({ "application/xml", "application/json",
				"application/atom+xml;type=entry" })
		@Description("Get a particular Atom event.")
		@NonNull
		AbstractEvent getEvent(@NonNull @PathParam("id") String id);
	}

	/**
	 * A description of an collection of events.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "Events")
	public static abstract class Events extends VersionedElement {
		/**
		 * @return The owner of the events in question.
		 */
		@XmlAttribute
		public abstract String getOwner();

		/**
		 * @return The actual list of events.
		 */
		@XmlElement
		public abstract List<AbstractEvent> getEvents();

		/**
		 * @param id
		 *            The identifier of a particular event.
		 * @return The details about that event.
		 */
		public abstract AbstractEvent getEvent(String id);
	}
}
