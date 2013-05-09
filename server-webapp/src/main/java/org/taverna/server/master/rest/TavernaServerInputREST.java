/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Roles.USER;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.port_description.InputDescription;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.exceptions.BadInputPortNameException;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.TavernaRun;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This represents how a Taverna Server workflow run's inputs looks to a RESTful
 * API.
 * 
 * @author Donal Fellows.
 */
@RolesAllowed(USER)
@Description("This represents how a Taverna Server workflow run's inputs looks to a RESTful API.")
public interface TavernaServerInputREST {
	/**
	 * @return A description of the various URIs to inputs associated with a
	 *         workflow run.
	 */
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json" })
	@Description("Describe the sub-URIs of this resource.")
	@NonNull
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
	@Path("expected")
	@Produces({ "application/xml", "application/json" })
	@Description("Describe the expected inputs of this workflow run.")
	@NonNull
	InputDescription getExpected();

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("expected")
	@Description("Produces the description of the expected inputs' operations.")
	Response expectedOptions();

	/**
	 * @return The Baclava file that will supply all the inputs to the workflow
	 *         run, or empty to indicate that no such file is specified.
	 */
	@GET
	@Path("baclava")
	@Produces("text/plain")
	@Description("Gives the Baclava file describing the inputs, or empty if individual files are used.")
	@NonNull
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
	@Path("baclava")
	@Consumes("text/plain")
	@Produces("text/plain")
	@Description("Sets the Baclava file describing the inputs.")
	@NonNull
	String setBaclavaFile(@NonNull String filename) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("baclava")
	@Description("Produces the description of the inputs' baclava operations.")
	Response baclavaOptions();

	/**
	 * Get what input is set for the specific input.
	 * 
	 * @param name
	 *            The input to set.
	 * @return A description of the input.
	 * @throws BadInputPortNameException
	 *             If no input with that name exists.
	 */
	@GET
	@Path("input/{name}")
	@Produces({ "application/xml", "application/json" })
	@Description("Gives a description of what is used to supply a particular input.")
	@NonNull
	InDesc getInput(@NonNull @PathParam("name") String name)
			throws BadInputPortNameException;

	/**
	 * Set what an input uses to provide data into the workflow run.
	 * 
	 * @param name
	 *            The name of the input.
	 * @param inputDescriptor
	 *            A description of the input
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
	@Path("input/{name}")
	@Consumes({ "application/xml", "application/json" })
	@Produces({ "application/xml", "application/json" })
	@Description("Sets the source for a particular input port.")
	@NonNull
	InDesc setInput(@NonNull @PathParam("name") String name,
			@NonNull InDesc inputDescriptor) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException,
			BadPropertyValueException, BadInputPortNameException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("input/{name}")
	@Description("Produces the description of the one input's operations.")
	Response inputOptions(@PathParam("name") String name);

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
			expected = new Uri(ui, "expected");
			baclava = new Uri(ui, "baclava");
			input = new ArrayList<Uri>();
			for (Input i : run.getInputs())
				input.add(new Uri(ui, "input/{name}", i.getName()));
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
		public InDesc(Input inputPort) {
			super(true);
			name = inputPort.getName();
			if (inputPort.getFile() != null) {
				assignment = new InDesc.File();
				assignment.contents = inputPort.getFile();
			} else {
				assignment = new InDesc.Value();
				assignment.contents = inputPort.getValue();
			}
		}

		/** The name of the port. */
		@XmlAttribute(required = false)
		public String name;

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
