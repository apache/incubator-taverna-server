/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Roles.USER;
import static org.taverna.server.master.rest.ContentTypes.JSON;
import static org.taverna.server.master.rest.ContentTypes.TEXT;
import static org.taverna.server.master.rest.ContentTypes.XML;
import static org.taverna.server.master.rest.TavernaServerInputREST.PathNames.BACLAVA;
import static org.taverna.server.master.rest.TavernaServerInputREST.PathNames.EXPECTED;
import static org.taverna.server.master.rest.TavernaServerInputREST.PathNames.ONE_INPUT;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.exceptions.BadInputPortNameException;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.port_description.InputDescription;

/**
 * This represents how a Taverna Server workflow run's inputs looks to a RESTful
 * API.
 * 
 * @author Donal Fellows.
 */
@RolesAllowed(USER)
@Description("This represents how a Taverna Server workflow run's inputs "
		+ "looks to a RESTful API.")
public interface TavernaServerInputREST {
	/**
	 * @return A description of the various URIs to inputs associated with a
	 *         workflow run.
	 */
	@GET
	@Path("/")
	@Produces({ XML, JSON })
	@Description("Describe the sub-URIs of this resource.")
	@Nonnull
	InputsDescriptor get();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("/")
	@Description("Produces the description of one run's inputs' operations.")
	Response options();

	/**
	 * @return A description of the various URIs to inputs associated with a
	 *         workflow run.
	 */
	@GET
	@Path(EXPECTED)
	@Produces({ XML, JSON })
	@Description("Describe the expected inputs of this workflow run.")
	@Nonnull
	InputDescription getExpected();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(EXPECTED)
	@Description("Produces the description of the expected inputs' operations.")
	Response expectedOptions();

	/**
	 * @return The Baclava file that will supply all the inputs to the workflow
	 *         run, or empty to indicate that no such file is specified.
	 */
	@GET
	@Path(BACLAVA)
	@Produces(TEXT)
	@Description("Gives the Baclava file describing the inputs, or empty if "
			+ "individual files are used.")
	@Nonnull
	String getBaclavaFile();

	/**
	 * Set the Baclava file that will supply all the inputs to the workflow run.
	 * 
	 * @param filename
	 *            The filename to set.
	 * @return The name of the Baclava file that was actually set.
	 * @throws NoUpdateException
	 *             If the user can't update the run.
	 * @throws BadStateChangeException
	 *             If the run is not Initialized.
	 * @throws FilesystemAccessException
	 *             If the filename starts with a <tt>/</tt> or if it contains a
	 *             <tt>..</tt> segment.
	 */
	@PUT
	@Path(BACLAVA)
	@Consumes(TEXT)
	@Produces(TEXT)
	@Description("Sets the Baclava file describing the inputs.")
	@Nonnull
	String setBaclavaFile(@Nonnull String filename) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(BACLAVA)
	@Description("Produces the description of the inputs' baclava operations.")
	Response baclavaOptions();

	/**
	 * Get what input is set for the specific input.
	 * 
	 * @param name
	 *            The input to set.
	 * @param uriInfo
	 *            About the URI used to access this resource.
	 * @return A description of the input.
	 * @throws BadInputPortNameException
	 *             If no input with that name exists.
	 */
	@GET
	@Path(ONE_INPUT)
	@Produces({ XML, JSON })
	@Description("Gives a description of what is used to supply a particular "
			+ "input.")
	@Nonnull
	InDesc getInput(@Nonnull @PathParam("name") String name,
			@Context UriInfo uriInfo) throws BadInputPortNameException;

	/**
	 * Set what an input uses to provide data into the workflow run.
	 * 
	 * @param name
	 *            The name of the input.
	 * @param inputDescriptor
	 *            A description of the input
	 * @param uriInfo
	 *            About the URI used to access this resource.
	 * @return A description of the input.
	 * @throws NoUpdateException
	 *             If the user can't update the run.
	 * @throws BadStateChangeException
	 *             If the run is not Initialized.
	 * @throws FilesystemAccessException
	 *             If a filename is being set and the filename starts with a
	 *             <tt>/</tt> or if it contains a <tt>..</tt> segment.
	 * @throws BadInputPortNameException
	 *             If no input with that name exists.
	 * @throws BadPropertyValueException
	 *             If some bad misconfiguration has happened.
	 */
	@PUT
	@Path(ONE_INPUT)
	@Consumes({ XML, JSON })
	@Produces({ XML, JSON })
	@Description("Sets the source for a particular input port.")
	@Nonnull
	InDesc setInput(@Nonnull @PathParam("name") String name,
			@Nonnull InDesc inputDescriptor, @Context UriInfo uriInfo) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException,
			BadPropertyValueException, BadInputPortNameException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path(ONE_INPUT)
	@Description("Produces the description of the one input's operations.")
	Response inputOptions(@PathParam("name") String name);

	interface PathNames {
		final String EXPECTED = "expected";
		final String BACLAVA = "baclava";
		final String ONE_INPUT = "input/{name}";
	}

	/**
	 * A description of the structure of inputs to a Taverna workflow run, done
	 * with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "runInputs")
	@XmlType(name = "TavernaRunInputs")
	public static class InputsDescriptor extends VersionedElement {
		/**
		 * Where to find a description of the expected inputs to this workflow
		 * run.
		 */
		public Uri expected;
		/**
		 * Where to find the overall Baclava document filename (if set).
		 */
		public Uri baclava;
		/**
		 * Where to find the details of inputs to particular ports (if set).
		 */
		public List<Uri> input;

		/**
		 * Make a blank description of the inputs.
		 */
		public InputsDescriptor() {
		}

		/**
		 * Make the description of the inputs.
		 * 
		 * @param ui
		 *            Information about the URIs to generate.
		 * @param run
		 *            The run whose inputs are to be described.
		 */
		public InputsDescriptor(UriInfo ui, TavernaRun run) {
			super(true);
			expected = new Uri(ui, EXPECTED);
			baclava = new Uri(ui, BACLAVA);
			input = new ArrayList<>();
			for (Input i : run.getInputs())
				input.add(new Uri(ui, ONE_INPUT, i.getName()));
		}
	}

	/**
	 * The Details of a particular input port's value assignment, done with
	 * JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "runInput")
	@XmlType(name = "InputDescription")
	public static class InDesc extends VersionedElement {
		/** Make a blank description of an input port. */
		public InDesc() {
		}

		/**
		 * Make a description of the given input port.
		 * 
		 * @param inputPort
		 */
		public InDesc(Input inputPort, UriInfo ui) {
			super(true);
			name = inputPort.getName();
			if (inputPort.getFile() != null) {
				assignment = new InDesc.File();
				assignment.contents = inputPort.getFile();
			} else {
				assignment = new InDesc.Value();
				assignment.contents = inputPort.getValue();
			}
			// .../runs/{id}/input/input/{name} ->
			// .../runs/{id}/input/expected#{name}
			UriBuilder ub = ui.getBaseUriBuilder();
			List<PathSegment> segments = ui.getPathSegments();
			for (PathSegment s : segments.subList(0, segments.size() - 2))
				ub.segment(s.getPath());
			ub.fragment(name);
			descriptorRef = new Uri(ub).ref;
		}

		/** The name of the port. */
		@XmlAttribute(required = false)
		public String name;
		/** Where the port is described. Ignored in user input. */
		@XmlAttribute(required = false)
		@XmlSchemaType(name = "anyURI")
		public URI descriptorRef;
		/** The character to use to split the input into a list. */
		@XmlAttribute(name = "listDelimiter", required = false)
		public String delimiter;

		/**
		 * Either a filename or a literal string, used to provide input to a
		 * workflow port.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "InputContents")
		public static abstract class AbstractContents {
			/**
			 * The contents of the description of the input port. Meaning not
			 * defined.
			 */
			@XmlValue
			public String contents;
		};

		/**
		 * The name of a file that provides input to the port. The
		 * {@link AbstractContents#contents contents} field is a filename.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class File extends AbstractContents {
		}

		/**
		 * The literal input to the port. The {@link AbstractContents#contents
		 * contents} field is a literal input value.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class Value extends AbstractContents {
		}

		/**
		 * A reference to a file elsewhere <i>on this server</i>. The
		 * {@link AbstractContents#contents contents} field is a URL to the file
		 * (using the RESTful notation).
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class Reference extends AbstractContents {
		}

		/**
		 * The assignment of input values to the port.
		 */
		@XmlElements({ @XmlElement(name = "file", type = File.class),
				@XmlElement(name = "reference", type = Reference.class),
				@XmlElement(name = "value", type = Value.class) })
		public AbstractContents assignment;
	}
}
