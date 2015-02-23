/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Namespaces.SERVER;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.rest.ContentTypes.JSON;
import static org.taverna.server.master.rest.ContentTypes.URI_LIST;
import static org.taverna.server.master.rest.ContentTypes.XML;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_CAPABILITIES;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_NOTIFIERS;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_OP_LIMIT;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_PERM_LIST;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_PERM_WF;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.POL_RUN_LIMIT;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.ROOT;
import static org.taverna.server.master.rest.TavernaServerREST.PathNames.RUNS;
import static org.taverna.server.master.rest.handler.Scufl2DocumentHandler.SCUFL2;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.common.Capability;
import org.taverna.server.master.common.RunReference;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.common.version.Version;
import org.taverna.server.master.exceptions.NoCreateException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.UnknownRunException;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.soap.TavernaServerSOAP;

/**
 * The REST service interface to Taverna 3 Server.
 * 
 * @author Donal Fellows
 * @see TavernaServerSOAP
 */
@RolesAllowed(USER)
@Description("This is REST service interface to Taverna " + Version.JAVA
		+ " Server.")
public interface TavernaServerREST {
	/**
	 * Produces the description of the service.
	 * 
	 * @param ui
	 *            About the URI being accessed.
	 * @return The description.
	 */
	@GET
	@Path(ROOT)
	@Produces({ XML, JSON })
	@Description("Produces the description of the service.")
	@Nonnull
	ServerDescription describeService(@Nonnull @Context UriInfo ui);

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ROOT)
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
	@Path(RUNS)
	@Produces({ XML, JSON })
	@RolesAllowed(USER)
	@Description("Produces a list of all runs visible to the user.")
	@Nonnull
	RunList listUsersRuns(@Nonnull @Context UriInfo ui);

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
	@Path(RUNS)
	@Consumes({ T2FLOW, SCUFL2, XML })
	@RolesAllowed(USER)
	@Description("Accepts (or not) a request to create a new run executing "
			+ "the given workflow.")
	@Nonnull
	Response submitWorkflow(@Nonnull Workflow workflow,
			@Nonnull @Context UriInfo ui) throws NoUpdateException;

	/**
	 * Accepts (or not) a request to create a new run executing the workflow at
	 * the given location.
	 * 
	 * @param workflowReference
	 *            The wrapped URI to workflow document to execute.
	 * @param ui
	 *            About the URI being POSTed to.
	 * @return A response to the POST describing what was created.
	 * @throws NoUpdateException
	 *             If the POST failed.
	 * @throw NoCreateException If the workflow couldn't be read into the server
	 *        or the engine rejects it.
	 */
	@POST
	@Path(RUNS)
	@Consumes(URI_LIST)
	@RolesAllowed(USER)
	@Description("Accepts a URL to a workflow to download and run. The URL "
			+ "must be hosted on a publicly-accessible service.")
	@Nonnull
	Response submitWorkflowByURL(@Nonnull List<URI> referenceList,
			@Nonnull @Context UriInfo ui) throws NoCreateException,
			NoUpdateException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(RUNS)
	@Description("Produces the description of the operations on the "
			+ "collection of runs.")
	Response runsOptions();

	/**
	 * @return A description of the policies supported by this server.
	 */
	@Path(POL)
	@Description("The policies supported by this server.")
	@Nonnull
	PolicyView getPolicyDescription();

	/**
	 * Get a particular named run resource.
	 * 
	 * @param runName
	 *            The name of the run.
	 * @param uriInfo
	 *            About the URI used to access this run.
	 * @return A RESTful delegate for the run.
	 * @throws UnknownRunException
	 *             If the run handle is unknown to the current user.
	 */
	@Path(RUNS + "/{runName}")
	@RolesAllowed(USER)
	@Description("Get a particular named run resource to dispatch to.")
	@Nonnull
	TavernaServerRunREST getRunResource(
			@Nonnull @PathParam("runName") String runName,
			@Nonnull @Context UriInfo uriInfo) throws UnknownRunException;

	/**
	 * Factored out path names used in the {@link TavernaServerREST} interface
	 * and related places.
	 * 
	 * @author Donal Fellows
	 */
	interface PathNames {
		public static final String ROOT = "/";
		public static final String RUNS = "runs";
		public static final String POL = "policy";
		public static final String POL_CAPABILITIES = "capabilities";
		public static final String POL_RUN_LIMIT = "runLimit";
		public static final String POL_OP_LIMIT = "operatingLimit";
		public static final String POL_PERM_WF = "permittedWorkflows";
		public static final String POL_PERM_LIST = "permittedListenerTypes";
		public static final String POL_NOTIFIERS = "enabledNotificationFabrics";
	}

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
			runs = new Uri(ui, RUNS);
			policy = new Uri(ui, false, POL);
			feed = new Uri(java.net.URI.create(base.replaceFirst("/rest$",
					"/feed")));
			if (interactionFeed != null && !interactionFeed.isEmpty())
				this.interactionFeed = new Uri(
						java.net.URI.create(interactionFeed));
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
		@Path(ROOT)
		@Produces({ XML, JSON })
		@Description("Describe the parts of this policy.")
		@Nonnull
		public PolicyDescription getDescription(@Nonnull @Context UriInfo ui);

		/**
		 * Gets the maximum number of simultaneous runs that the user may
		 * create. The <i>actual</i> number they can create may be lower than
		 * this. If this number is lower than the number they currently have,
		 * they will be unable to create any runs at all.
		 * 
		 * @return The maximum number of existing runs.
		 */
		@GET
		@Path(POL_RUN_LIMIT)
		@Produces("text/plain")
		@RolesAllowed(USER)
		@Description("Gets the maximum number of simultaneous runs in any "
				+ "state that the user may create.")
		@Nonnull
		public int getMaxSimultaneousRuns();

		/**
		 * Gets the maximum number of simultaneous
		 * {@linkplain org.taverna.server.master.common.Status.Operating
		 * operating} runs that the user may create. The <i>actual</i> number
		 * they can start may be lower than this. If this number is lower than
		 * the number they currently have, they will be unable to start any runs
		 * at all.
		 * 
		 * @return The maximum number of operating runs.
		 */
		@GET
		@Path(POL_OP_LIMIT)
		@Produces("text/plain")
		@RolesAllowed(USER)
		@Description("Gets the maximum number of simultaneously operating "
				+ "runs that the user may have. Note that this is often a "
				+ "global limit; it does not represent a promise that a "
				+ "particular user may be able to have that many operating "
				+ "runs at once.")
		public int getMaxOperatingRuns();

		/**
		 * Gets the list of permitted workflows. Any workflow may be submitted
		 * if the list is empty, otherwise it must be one of the workflows on
		 * this list.
		 * 
		 * @return The list of workflow documents.
		 */
		@GET
		@Path(POL_PERM_WF)
		@Produces({ XML, JSON })
		@RolesAllowed(USER)
		@Description("Gets the list of permitted workflows.")
		@Nonnull
		public PermittedWorkflows getPermittedWorkflows();

		/**
		 * Gets the list of permitted event listener types. All event listeners
		 * must be of a type described on this list.
		 * 
		 * @return The types of event listeners allowed.
		 */
		@GET
		@Path(POL_PERM_LIST)
		@Produces({ XML, JSON })
		@RolesAllowed(USER)
		@Description("Gets the list of permitted event listener types.")
		@Nonnull
		public PermittedListeners getPermittedListeners();

		/**
		 * Gets the list of supported, enabled notification fabrics. Each
		 * corresponds (approximately) to a protocol, e.g., email.
		 * 
		 * @return List of notifier names; each is the scheme of a notification
		 *         destination URI.
		 */
		@GET
		@Path(POL_NOTIFIERS)
		@Produces({ XML, JSON })
		@RolesAllowed(USER)
		@Description("Gets the list of supported, enabled notification "
				+ "fabrics. Each corresponds (approximately) to a protocol, "
				+ "e.g., email.")
		@Nonnull
		public EnabledNotificationFabrics getEnabledNotifiers();

		@GET
		@Path(POL_CAPABILITIES)
		@Produces({ XML, JSON })
		@RolesAllowed(USER)
		@Description("Gets a description of the capabilities supported by "
				+ "this installation of Taverna Server.")
		@Nonnull
		public CapabilityList getCapabilities();

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
			 * Where to go to find out about the maximum number of operating
			 * runs.
			 */
			public Uri operatingLimit;
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

			public Uri capabilities;

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
				runLimit = new Uri(ui, false, POL_RUN_LIMIT);
				operatingLimit = new Uri(ui, false, POL_OP_LIMIT);
				permittedWorkflows = new Uri(ui, false, POL_PERM_WF);
				permittedListenerTypes = new Uri(ui, false, POL_PERM_LIST);
				enabledNotificationFabrics = new Uri(ui, false, POL_NOTIFIERS);
				capabilities = new Uri(ui, false, POL_CAPABILITIES);
			}
		}

		/**
		 * A list of Taverna Server capabilities.
		 * 
		 * @author Donal Fellows
		 */
		@XmlRootElement(name = "capabilities")
		@XmlType(name = "")
		public static class CapabilityList {
			@XmlElement(name = "capability", namespace = SERVER)
			public List<Capability> capability = new ArrayList<>();
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
		public List<URI> workflow;

		/**
		 * Make an empty list of permitted workflows.
		 */
		public PermittedWorkflows() {
			workflow = new ArrayList<>();
		}

		/**
		 * Make a list of permitted workflows.
		 * 
		 * @param permitted
		 */
		public PermittedWorkflows(List<URI> permitted) {
			if (permitted == null)
				workflow = new ArrayList<>();
			else
				workflow = new ArrayList<>(permitted);
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
			type = new ArrayList<>();
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
			run = new ArrayList<>();
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
			run = new ArrayList<>(runs.size());
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
			notifier = new ArrayList<>();
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
		@Produces("application/atom+xml;type=feed")
		@Description("Get an Atom feed for the user's events.")
		@Nonnull
		Feed getFeed(@Context UriInfo ui);

		/**
		 * @param id
		 *            The identifier for a particular event.
		 * @return the details about the given event.
		 */
		@GET
		@Path("{id}")
		@Produces("application/atom+xml;type=entry")
		@Description("Get a particular Atom event.")
		@Nonnull
		Entry getEvent(@Nonnull @PathParam("id") String id);
	}

	/**
	 * A reference to a workflow hosted on some public HTTP server.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "workflowurl")
	@XmlType(name = "WorkflowReference")
	public static class WorkflowReference {
		@XmlValue
		@XmlSchemaType(name = "anyURI")
		public URI url;
	}
}
