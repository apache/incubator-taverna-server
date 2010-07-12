package org.taverna.server.master.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.cxf.jaxrs.ext.Description;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.exceptions.BadPropertyValueException;
import org.taverna.server.master.exceptions.BadStateChangeException;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * This represents how a Taverna Server workflow run's inputs looks to a RESTful
 * API.
 * 
 * @author Donal Fellows.
 */
@Description("This represents how a Taverna Server workflow run's inputs looks to a RESTful API.")
public interface TavernaServerInputREST {
	/**
	 * @param ui
	 *            About the URI used to access this resource.
	 * @return A description of the various URIs to inputs associated with a
	 *         workflow run.
	 */
	@GET
	@Path("/")
	@Produces( { "application/xml", "application/json" })
	@Description("Describe the sub-URIs of this resource.")
	public InputsDescriptor get(@Context UriInfo ui);

	/**
	 * @return The Baclava file that will supply all the inputs to the workflow
	 *         run, or empty to indicate that no such file is specified.
	 */
	@GET
	@Path("baclava")
	@Produces("text/plain")
	@Description("Gives the Baclava file describing the inputs, or empty if individual files are used.")
	public String getBaclavaFile();

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
	public String setBaclavaFile(String filename) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException;

	/**
	 * Get what input is set for the specific input.
	 * 
	 * @param name
	 *            The input to set.
	 * @return A description of the input.
	 * @throws BadPropertyValueException
	 *             If no input with that name exists.
	 */
	@GET
	@Path("input/{name}")
	@Produces( { "application/xml", "application/json" })
	@Description("Gives a description of what is used to supply a particular input.")
	public InDesc getInput(@PathParam("name") String name)
			throws BadPropertyValueException;

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
	 * @throws BadPropertyValueException
	 *             If no input with that name exists.
	 */
	@PUT
	@Path("input/{name}")
	@Consumes( { "application/xml", "application/json" })
	@Description("Sets the source for a particular input port.")
	public InDesc setInput(@PathParam("name") String name,
			InDesc inputDescriptor) throws NoUpdateException,
			BadStateChangeException, FilesystemAccessException,
			BadPropertyValueException;

	/**
	 * A description of the structure of inputs to a Taverna workflow run, done
	 * with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement(name = "runInputs")
	@XmlType(name = "TavernaRunInputs")
	public static class InputsDescriptor {
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
			baclava = new Uri(ui, "baclava");
			input = new ArrayList<Uri>();
			for (Input i : run.getInputs()) {
				input.add(new Uri(ui, "input/{name}", i.getName()));
			}
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
	public static class InDesc {
		/** Make a blank description of an input port. */
		public InDesc() {
		}

		/**
		 * Make a description of the given input port.
		 * 
		 * @param inputPort
		 */
		public InDesc(Input inputPort) {
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
		 * The the literal input to the port. The
		 * {@link AbstractContents#contents contents} field is a literal input
		 * value.
		 * 
		 * @author Donal Fellows
		 */
		@XmlType(name = "")
		public static class Value extends AbstractContents {
		}

		/**
		 * The assignment of input values to the port.
		 */
		@XmlElements( { @XmlElement(name = "file", type = File.class),
				@XmlElement(name = "value", type = Value.class) })
		public AbstractContents assignment;
	}
}
