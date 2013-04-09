/*
 * Copyright (C) 2010 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.common;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.taverna.server.master.interfaces.Input;
import org.taverna.server.master.interfaces.TavernaRun;

/**
 * A description of the inputs to a workflow, described using JAXB.
 * 
 * @author Donal Fellows
 */
@XmlRootElement(name = "inputConfiguration")
@XmlType(name = "InputConfigurationDescription")
public class InputDescription extends VersionedElement {
	/**
	 * The Baclava file handling the description of the elements. May be
	 * omitted/<tt>null</tt>.
	 */
	@XmlElement(required = false)
	public String baclavaFile;
	/**
	 * The port/value assignment.
	 */
	@XmlElement(nillable = false)
	public List<Port> port = new ArrayList<Port>();

	/**
	 * Make a blank input description.
	 */
	public InputDescription() {
	}

	/**
	 * Make an input description suitable for the given workflow run.
	 * 
	 * @param run
	 */
	public InputDescription(TavernaRun run) {
		super(true);
		baclavaFile = run.getInputBaclavaFile();
		if (baclavaFile == null)
			for (Input i : run.getInputs())
				port.add(new Port(i));
	}

	/**
	 * The type of a single port description.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "PortConfigurationDescription")
	public static class Port {
		/**
		 * The name of this port.
		 */
		@XmlAttribute(name = "portName", required = true)
		public String name;
		/**
		 * The file assigned to this port.
		 */
		@XmlAttribute(name = "portFile", required = false)
		public String file;
		/**
		 * The value assigned to this port.
		 */
		@XmlValue
		public String value;

		/**
		 * Make a blank port description.
		 */
		public Port() {
		}

		/**
		 * Make a port description suitable for the given input.
		 * 
		 * @param input
		 */
		public Port(Input input) {
			name = input.getName();
			if (input.getFile() != null) {
				file = input.getFile();
				value = "";
			} else {
				file = null;
				value = input.getValue();
			}
		}
	}
}
