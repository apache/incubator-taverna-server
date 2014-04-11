/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.joda.time.format.ISODateTimeFormat.basicDateTime;
import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.interaction.InteractionFeedSupport.FEED_URL_DIR;
import static org.taverna.server.master.rest.ContentTypes.JSON;
import static org.taverna.server.master.rest.ContentTypes.ROBUNDLE;
import static org.taverna.server.master.rest.ContentTypes.TEXT;
import static org.taverna.server.master.rest.ContentTypes.XML;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.DIR;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.GENERATE_PROVENANCE;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.IN;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.LISTEN;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.LOG;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.NAME;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.OUT;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.ROOT;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.RUNBUNDLE;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.SEC;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.STATUS;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.STDERR;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.STDOUT;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.T_CREATE;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.T_EXPIRE;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.T_FINISH;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.T_START;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.USAGE;
import static org.taverna.server.master.rest.TavernaServerRunREST.PathNames.WF;
import static org.taverna.server.master.rest.handler.T2FlowDocumentHandler.T2FLOW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.joda.time.format.DateTimeFormatter;
import org.taverna.server.master.common.Namespaces;
import org.taverna.server.master.common.Status;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.exceptions.NotOwnerException;
import org.taverna.server.master.interfaces.Listener;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.port_description.OutputDescription;

/**
 * This represents how a Taverna Server workflow run looks to a RESTful API.
 * 
 * @author Donal Fellows.
 */
@Description("This represents how a Taverna Server workflow run looks to a "
		+ "RESTful API.")
@RolesAllowed(USER)
public interface TavernaServerRunREST {
	/**
	 * Describes a workflow run.
	 * 
	 * @param ui
	 *            About the URI used to access this resource.
	 * @return The description.
	 */
	@GET
	@Path(ROOT)
	@Description("Describes a workflow run.")
	@Produces({ XML, JSON })
	@Nonnull
	public RunDescription getDescription(@Nonnull @Context UriInfo ui);

	/**
	 * Deletes a workflow run.
	 * 
	 * @return An HTTP response to the deletion.
	 * @throws NoUpdateException
	 *             If the user may see the handle but may not delete it.
	 */
	@DELETE
	@Path(ROOT)
	@Description("Deletes a workflow run.")
	@Nonnull
	public Response destroy() throws NoUpdateException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ROOT)
	@Description("Produces the description of the run.")
	Response runOptions();

	/**
	 * Returns the workflow document used to create the workflow run.
	 * 
	 * @return The workflow document.
	 */
	@GET
	@Path(WF)
	@Produces({ T2FLOW, XML, JSON })
	@Description("Gives the workflow document used to create the workflow run.")
	@Nonnull
	public Workflow getWorkflow();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(WF)
	@Description("Produces the description of the run workflow.")
	Response workflowOptions();

	/** Get the workflow name. */
	@GET
	@Path(NAME)
	@Produces(TEXT)
	@Description("Gives the descriptive name of the workflow run.")
	@Nonnull
	public String getName();

	/**
	 * Set the workflow name.
	 * 
	 * @throws NoUpdateException
	 *             If the user is not permitted to change the workflow.
	 */
	@PUT
	@Path(NAME)
	@Consumes(TEXT)
	@Produces(TEXT)
	@Description("Set the descriptive name of the workflow run. Note that "
			+ "this value may be arbitrarily truncated by the implementation.")
	@Nonnull
	public String setName(String name) throws NoUpdateException;

	/** Produce the workflow name HTTP operations. */
	@OPTIONS
	@Path(NAME)
	@Description("Produces the description of the operations on the run's "
			+ "descriptive name.")
	@Nonnull
	Response nameOptions();

	/**
	 * Returns a resource that represents the workflow run's security
	 * properties. These may only be accessed by the owner.
	 * 
	 * @return The security resource.
	 * @throws NotOwnerException
	 *             If the accessing principal isn't the owning principal.
	 */
	@Path(SEC)
	@Description("Access the workflow run's security.")
	@Nonnull
	public TavernaServerSecurityREST getSecurity() throws NotOwnerException;

	/**
	 * Returns the time when the workflow run becomes eligible for automatic
	 * deletion.
	 * 
	 * @return When the run expires.
	 */
	@GET
	@Path(T_EXPIRE)
	@Produces(TEXT)
	@Description("Gives the time when the workflow run becomes eligible for "
			+ "automatic deletion.")
	@Nonnull
	public String getExpiryTime();

	/**
	 * Sets the time when the workflow run becomes eligible for automatic
	 * deletion.
	 * 
	 * @param expiry
	 *            When the run will expire.
	 * @return When the run will actually expire.
	 * @throws NoUpdateException
	 *             If the current user is not permitted to manage the lifetime
	 *             of the run.
	 */
	@PUT
	@Path(T_EXPIRE)
	@Consumes(TEXT)
	@Produces(TEXT)
	@Description("Sets the time when the workflow run becomes eligible for "
			+ "automatic deletion.")
	@Nonnull
	public String setExpiryTime(@Nonnull String expiry)
			throws NoUpdateException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(T_EXPIRE)
	@Description("Produces the description of the run expiry.")
	Response expiryOptions();

	/**
	 * Returns the time when the workflow run was created.
	 * 
	 * @return When the run was first submitted to the server.
	 */
	@GET
	@Path(T_CREATE)
	@Produces(TEXT)
	@Description("Gives the time when the workflow run was first submitted "
			+ "to the server.")
	@Nonnull
	public String getCreateTime();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(T_CREATE)
	@Description("Produces the description of the run create time.")
	Response createTimeOptions();

	/**
	 * Returns the time when the workflow run was started (through a user-driven
	 * state change).
	 * 
	 * @return When the run was started, or <tt>null</tt>.
	 */
	@GET
	@Path(T_START)
	@Produces(TEXT)
	@Description("Gives the time when the workflow run was started, or an "
			+ "empty string if the run has not yet started.")
	@Nonnull
	public String getStartTime();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(T_START)
	@Description("Produces the description of the run start time.")
	Response startTimeOptions();

	/**
	 * Returns the time when the workflow run was detected to have finished.
	 * 
	 * @return When the run finished, or <tt>null</tt>.
	 */
	@GET
	@Path(T_FINISH)
	@Produces(TEXT)
	@Description("Gives the time when the workflow run was first detected as "
			+ "finished, or an empty string if it has not yet finished "
			+ "(including if it has never started).")
	@Nonnull
	public String getFinishTime();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(T_FINISH)
	@Description("Produces the description of the run finish time.")
	Response finishTimeOptions();

	/**
	 * Gets the current status of the workflow run.
	 * 
	 * @return The status code.
	 */
	@GET
	@Path(STATUS)
	@Produces(TEXT)
	@Description("Gives the current status of the workflow run.")
	@Nonnull
	public String getStatus();

	/**
	 * Sets the status of the workflow run. This does nothing if the status code
	 * is the same as the run's current state.
	 * 
	 * @param status
	 *            The new status code.
	 * @return Description of what status the run is actually in, or a 202 to
	 *         indicate that things are still changing.
	 * @throws NoUpdateException
	 *             If the current user is not permitted to update the run.
	 * @throws BadStateChangeException
	 *             If the state cannot be modified in the manner requested.
	 */
	@PUT
	@Path(STATUS)
	@Consumes(TEXT)
	@Produces(TEXT)
	@Description("Attempts to update the status of the workflow run.")
	@Nonnull
	public Response setStatus(@Nonnull String status) throws NoUpdateException,
			BadStateChangeException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(STATUS)
	@Description("Produces the description of the run status.")
	Response statusOptions();

	/**
	 * Get the working directory of this workflow run.
	 * 
	 * @return A RESTful delegate for the working directory.
	 */
	@Path(DIR)
	@Description("Get the working directory of this workflow run.")
	@Nonnull
	public TavernaServerDirectoryREST getWorkingDirectory();

	/**
	 * Get the event listeners attached to this workflow run.
	 * 
	 * @return A RESTful delegate for the list of listeners.
	 */
	@Path(LISTEN)
	@Description("Get the event listeners attached to this workflow run.")
	@Nonnull
	public TavernaServerListenersREST getListeners();

	/**
	 * Get a delegate for working with the inputs to this workflow run.
	 * 
	 * @param ui
	 *            About the URI used to access this resource.
	 * @return A RESTful delegate for the inputs.
	 */
	@Path(IN)
	@Description("Get the inputs to this workflow run.")
	@Nonnull
	public TavernaServerInputREST getInputs(@Nonnull @Context UriInfo ui);

	/**
	 * Get the output Baclava file for this workflow run.
	 * 
	 * @return The filename, or empty string to indicate that the outputs will
	 *         be written to the <tt>out</tt> directory.
	 */
	@GET
	@Path(OUT)
	@Produces(TEXT)
	@Description("Gives the Baclava file where output will be written; empty "
			+ "means use multiple simple files in the out directory.")
	@Nonnull
	public String getOutputFile();

	/**
	 * Get a description of the outputs.
	 * 
	 * @param ui
	 *            About the URI used to access this operation.
	 * @return A description of the outputs (higher level than the filesystem).
	 * @throws BadStateChangeException
	 *             If the run is in the {@link Status#Initialized Initialized}
	 *             state.
	 * @throws FilesystemAccessException
	 *             If problems occur when accessing the filesystem.
	 * @throws NoDirectoryEntryException
	 *             If things are odd in the filesystem.
	 */
	@GET
	@Path(OUT)
	@Produces({ XML, JSON })
	@Description("Gives a description of the outputs, as currently understood")
	@Nonnull
	public OutputDescription getOutputDescription(@Nonnull @Context UriInfo ui)
			throws BadStateChangeException, FilesystemAccessException,
			NoDirectoryEntryException;

	/**
	 * Set the output Baclava file for this workflow run.
	 * 
	 * @param filename
	 *            The Baclava file to use, or empty to make the outputs be
	 *            written to individual files in the <tt>out</tt> subdirectory
	 *            of the working directory.
	 * @return The Baclava file as actually set.
	 * @throws NoUpdateException
	 *             If the current user is not permitted to update the run.
	 * @throws FilesystemAccessException
	 *             If the filename is invalid (starts with <tt>/</tt> or
	 *             contains a <tt>..</tt> segment).
	 * @throws BadStateChangeException
	 *             If the workflow is not in the Initialized state.
	 */
	@PUT
	@Path(OUT)
	@Consumes(TEXT)
	@Produces(TEXT)
	@Description("Sets the Baclava file where output will be written; empty "
			+ "means use multiple simple files in the out directory.")
	@Nonnull
	public String setOutputFile(@Nonnull String filename)
			throws NoUpdateException, FilesystemAccessException,
			BadStateChangeException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(OUT)
	@Description("Produces the description of the run output.")
	Response outputOptions();

	/**
	 * Get a handle to the interaction feed.
	 * 
	 * @return
	 */
	@Path(FEED_URL_DIR)
	@Description("Access the interaction feed for the workflow run.")
	@Nonnull
	InteractionFeedREST getInteractionFeed();

	/**
	 * @return The stdout for the workflow run, or empty string if the run has
	 *         not yet started.
	 * @throws NoListenerException
	 */
	@GET
	@Path(STDOUT)
	@Description("Return the stdout for the workflow run.")
	@Produces(TEXT)
	@Nonnull
	String getStdout() throws NoListenerException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(STDOUT)
	@Description("Return the stdout for the workflow run.")
	Response stdoutOptions();

	/**
	 * @return The stderr for the workflow run, or empty string if the run has
	 *         not yet started.
	 * @throws NoListenerException
	 */
	@GET
	@Path(STDERR)
	@Description("Return the stderr for the workflow run.")
	@Produces(TEXT)
	@Nonnull
	String getStderr() throws NoListenerException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(STDERR)
	@Description("Return the stderr for the workflow run.")
	Response stderrOptions();

	/**
	 * @return The usage record for the workflow run, wrapped in a Response, or
	 *         "empty content" if the run has not yet finished.
	 * @throws NoListenerException
	 * @throws JAXBException
	 */
	@GET
	@Path(USAGE)
	@Description("Return the usage record for the workflow run.")
	@Produces(XML)
	@Nonnull
	Response getUsage() throws NoListenerException, JAXBException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(USAGE)
	@Description("Return the usage record for the workflow run.")
	Response usageOptions();

	/**
	 * @return The log for the workflow run, or empty string if the run has not
	 *         yet started.
	 */
	@GET
	@Path(LOG)
	@Description("Return the log for the workflow run.")
	@Produces(TEXT)
	@Nonnull
	Response getLogContents();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(LOG)
	@Description("Return the log for the workflow run.")
	Response logOptions();

	/**
	 * @return The log for the workflow run, or empty string if the run has not
	 *         yet started.
	 */
	@GET
	@Path(RUNBUNDLE)
	@Description("Return the run bundle for the workflow run.")
	@Produces(ROBUNDLE)
	@Nonnull
	Response getRunBundle();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(RUNBUNDLE)
	@Description("Return the run bundle for the workflow run.")
	Response runBundleOptions();

	/**
	 * @return Whether to create the run bundle for the workflow run. Only
	 *         usefully set-able before the start of the run.
	 */
	@GET
	@Path(GENERATE_PROVENANCE)
	@Description("Whether to create the run bundle for the workflow run.")
	@Produces(TEXT)
	@Nonnull
	boolean getGenerateProvenance();

	/**
	 * @param provenanceFlag
	 *            Whether to create the run bundle for the workflow run. Only
	 *            usefully set-able before the start of the run.
	 * @return What it was actually set to.
	 * @throws NoUpdateException 
	 */
	@PUT
	@Path(GENERATE_PROVENANCE)
	@Description("Whether to create the run bundle for the workflow run.")
	@Consumes(TEXT)
	@Produces(TEXT)
	@Nonnull
	boolean setGenerateProvenance(boolean provenanceFlag) throws NoUpdateException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(GENERATE_PROVENANCE)
	@Description("Whether to create the run bundle for the workflow run.")
	Response generateProvenanceOptions();

	/**
	 * Factored out path names used in the {@link TavernaServerRunREST}
	 * interface and related places.
	 * 
	 * @author Donal Fellows
	 */
	interface PathNames {
		public static final String ROOT = "/";
		public static final String WF = "workflow";
		public static final String DIR = "wd";
		public static final String NAME = "name";
		public static final String T_EXPIRE = "expiry";
		public static final String T_CREATE = "createTime";
		public static final String T_START = "startTime";
		public static final String T_FINISH = "finishTime";
		public static final String STATUS = "status";
		public static final String IN = "input";
		public static final String OUT = "output";
		public static final String LISTEN = "listeners";
		public static final String SEC = "security";
		public static final String STDOUT = "stdout";
		public static final String STDERR = "stderr";
		public static final String USAGE = "usage";
		public static final String LOG = "log";
		public static final String RUNBUNDLE = "run-bundle";
		public static final String GENERATE_PROVENANCE = "generate-provenance";
	}

	/**
	 * The description of where everything is in a RESTful view of a workflow
	 * run. Done with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class RunDescription extends VersionedElement {
		/** The identity of the owner of the workflow run. */
		@XmlAttribute(namespace = Namespaces.SERVER_REST)
		public String owner;
		/** The description of the expiry. */
		public Expiry expiry;
		/** The location of the creation workflow description. */
		public Uri creationWorkflow;
		/** The location of the creation time property. */
		public Uri createTime;
		/** The location of the start time property. */
		public Uri startTime;
		/** The location of the finish time property. */
		public Uri finishTime;
		/** The location of the status description. */
		public Uri status;
		/** The location of the working directory. */
		public Uri workingDirectory;
		/** The location of the inputs. */
		public Uri inputs;
		/** The location of the Baclava output. */
		public Uri output;
		/** The location of the security context. */
		public Uri securityContext;
		/** The list of listeners. */
		public ListenerList listeners;
		/** The location of the interaction feed. */
		public Uri interaction;
		/** The name of the run. */
		public Uri name;
		/** The stdout of the run. */
		public Uri stdout;
		/** The stderr of the run. */
		public Uri stderr;
		/** The usage record for the run. */
		public Uri usage;
		/** The log from the run. */
		public Uri log;
		/** The bundle describing the run. */
		@XmlElement(name = RUNBUNDLE)
		public Uri runBundle;
		/** Whether to generate a bundle describing the run. */
		@XmlElement(name = GENERATE_PROVENANCE)
		public Uri generateProvenance;

		/**
		 * How to describe a run's expiry.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class Expiry {
			/**
			 * Where to go to read the exiry
			 */
			@XmlAttribute(name = "href", namespace = Namespaces.XLINK)
			@XmlSchemaType(name = "anyURI")
			public URI ref;
			/**
			 * What the expiry currently is.
			 */
			@XmlValue
			public String timeOfDeath;

			/**
			 * Make a blank expiry description.
			 */
			public Expiry() {
			}

			private static DateTimeFormatter dtf;

			Expiry(TavernaRun r, UriInfo ui, String path, String... parts) {
				ref = fromUri(new Uri(ui, true, path, parts).ref).build();
				if (dtf == null)
					dtf = basicDateTime();
				timeOfDeath = dtf.print(r.getExpiry().getTime());
			}
		}

		/**
		 * The description of a list of listeners attached to a run.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class ListenerList extends Uri {
			/**
			 * The references to the individual listeners.
			 */
			public List<Uri> listener;

			/**
			 * An empty description of listeners.
			 */
			public ListenerList() {
				listener = new ArrayList<>();
			}

			/**
			 * @param r
			 *            The run whose listeners we're talking about.
			 * @param ub
			 *            Uri factory; must've been secured
			 */
			private ListenerList(TavernaRun r, UriBuilder ub) {
				super(ub);
				listener = new ArrayList<>(r.getListeners().size());
				UriBuilder pathUB = ub.clone().path("{name}");
				for (Listener l : r.getListeners())
					listener.add(new Uri(pathUB.build(l.getName())));
			}

			/**
			 * @param run
			 *            The run whose listeners we're talking about.
			 * @param ui
			 *            The source of information about URIs.
			 * @param path
			 *            Where we are relative to the URI source.
			 * @param parts
			 *            Anything required to fill out the path.
			 */
			ListenerList(TavernaRun run, UriInfo ui, String path,
					String... parts) {
				this(run, secure(fromUri(new Uri(ui, path, parts).ref)));
			}
		}

		/**
		 * An empty description of a run.
		 */
		public RunDescription() {
		}

		/**
		 * A description of a particular run.
		 * 
		 * @param run
		 *            The run to describe.
		 * @param ui
		 *            The factory for URIs.
		 */
		public RunDescription(TavernaRun run, UriInfo ui) {
			super(true);
			creationWorkflow = new Uri(ui, WF);
			expiry = new Expiry(run, ui, T_EXPIRE);
			status = new Uri(ui, STATUS);
			workingDirectory = new Uri(ui, DIR);
			listeners = new ListenerList(run, ui, LISTEN);
			securityContext = new Uri(ui, SEC);
			inputs = new Uri(ui, IN);
			output = new Uri(ui, OUT);
			createTime = new Uri(ui, T_CREATE);
			startTime = new Uri(ui, T_START);
			finishTime = new Uri(ui, T_FINISH);
			interaction = new Uri(ui, FEED_URL_DIR);
			name = new Uri(ui, NAME);
			owner = run.getSecurityContext().getOwner().getName();
			stdout = new Uri(ui, STDOUT);
			stderr = new Uri(ui, STDERR);
			usage = new Uri(ui, USAGE);
			log = new Uri(ui, LOG);
			runBundle = new Uri(ui, RUNBUNDLE);
			generateProvenance = new Uri(ui, GENERATE_PROVENANCE);
		}
	}
}
