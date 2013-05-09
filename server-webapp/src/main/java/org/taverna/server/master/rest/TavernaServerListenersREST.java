/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.rest;

import static org.taverna.server.master.common.Namespaces.XLINK;
import static org.taverna.server.master.common.Roles.USER;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.jaxrs.model.wadl.Description;
import org.taverna.server.master.common.Uri;
import org.taverna.server.master.common.VersionedElement;
import org.taverna.server.master.exceptions.NoListenerException;
import org.taverna.server.master.exceptions.NoUpdateException;
import org.taverna.server.master.interfaces.Listener;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This represents <i>all</i> the event listeners attached to a workflow run.
 * 
 * @author Donal Fellows
 * @see TavernaServerListenerREST
 */
@RolesAllowed(USER)
@Description("This represents all the event listeners attached to a workflow run.")
public interface TavernaServerListenersREST {
	/**
	 * Get the listeners installed in the workflow run.
	 * 
	 * @param ui
	 *            About how this method was called.
	 * @return A list of descriptions of listeners.
	 */
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json" })
	@Description("Get the listeners installed in the workflow run.")
	@NonNull
	Listeners getDescription(@NonNull @Context UriInfo ui);

	/**
	 * Add a new event listener to the named workflow run.
	 * 
	 * @param typeAndConfiguration
	 *            What type of run should be created, and how should it be
	 *            configured.
	 * @param ui
	 *            About how this method was called.
	 * @return An HTTP response to the creation request.
	 * @throws NoUpdateException
	 *             If the user is not permitted to update the run.
	 * @throws NoListenerException
	 *             If no listener with the given type exists, or if the
	 *             configuration is unacceptable in some way.
	 */
	@POST
	@Path("/")
	@Consumes({ "application/xml", "application/json" })
	@Description("Add a new event listener to the named workflow run.")
	@NonNull
	Response addListener(@NonNull ListenerDefinition typeAndConfiguration,
			@NonNull @Context UriInfo ui) throws NoUpdateException,
			NoListenerException;

	/** Get an outline of the operations supported. */
	@OPTIONS
	@Path("/")
	@Description("Produces the description of the run listeners' operations.")
	Response listenersOptions();

	/**
	 * Resolve a particular listener from its name.
	 * 
	 * @param name
	 *            The name of the listener to look up.
	 * @return The listener's delegate in the REST world.
	 * @throws NoListenerException
	 *             If no listener with the given name exists.
	 */
	@Path("{name}")
	@Description("Resolve a particular listener from its name.")
	@NonNull
	TavernaServerListenerREST getListener(
			@NonNull @PathParam("name") String name) throws NoListenerException;

	/**
	 * This represents a single event listener attached to a workflow run.
	 * 
	 * @author Donal Fellows
	 * @see TavernaServerListenersREST
	 * @see Property
	 */
	@RolesAllowed(USER)
	@Description("This represents a single event listener attached to a workflow run.")
	public interface TavernaServerListenerREST {
		/**
		 * Get the description of this listener.
		 * 
		 * @param ui
		 *            Information about this request.
		 * @return A description document.
		 */
		@GET
		@Path("/")
		@Produces({ "application/xml", "application/json" })
		@Description("Get the description of this listener.")
		@NonNull
		ListenerDescription getDescription(@NonNull @Context UriInfo ui);

		/** Get an outline of the operations supported. */
		@OPTIONS
		@Path("/")
		@Description("Produces the description of one run listener's operations.")
		Response listenerOptions();

		/**
		 * Get the configuration for the given event listener that is attached
		 * to a workflow run.
		 * 
		 * @return The configuration of the listener.
		 */
		@GET
		@Path("configuration")
		@Produces("text/plain")
		@Description("Get the configuration for the given event listener that is attached to a workflow run.")
		@NonNull
		String getConfiguration();

		/** Get an outline of the operations supported. */
		@OPTIONS
		@Path("configuration")
		@Description("Produces the description of one run listener's configuration's operations.")
		Response configurationOptions();

		/**
		 * Get the list of properties supported by a given event listener
		 * attached to a workflow run.
		 * 
		 * @param ui
		 *            Information about this request.
		 * @return The list of property names.
		 */
		@GET
		@Path("properties")
		@Produces({ "application/xml", "application/json" })
		@Description("Get the list of properties supported by a given event listener attached to a workflow run.")
		@NonNull
		Properties getProperties(@NonNull @Context UriInfo ui);

		/** Get an outline of the operations supported. */
		@OPTIONS
		@Path("properties")
		@Description("Produces the description of one run listener's properties' operations.")
		Response propertiesOptions();

		/**
		 * Get an object representing a particular property.
		 * 
		 * @param propertyName
		 * @return The property delegate.
		 * @throws NoListenerException
		 *             If there is no such property.
		 */
		@Path("properties/{propertyName}")
		@Description("Get an object representing a particular property.")
		@NonNull
		Property getProperty(
				@NonNull @PathParam("propertyName") String propertyName)
				throws NoListenerException;
	}

	/**
	 * This represents a single property attached of an event listener.
	 * 
	 * @author Donal Fellows
	 */
	@RolesAllowed(USER)
	@Description("This represents a single property attached of an event listener.")
	public interface Property {
		/**
		 * Get the value of the particular property of an event listener
		 * attached to a workflow run.
		 * 
		 * @return The value of the property.
		 */
		@GET
		@Path("/")
		@Produces("text/plain")
		@Description("Get the value of the particular property of an event listener attached to a workflow run.")
		@NonNull
		String getValue();

		/**
		 * Set the value of the particular property of an event listener
		 * attached to a workflow run. Changing the value of the property may
		 * cause the listener to alter its behaviour significantly.
		 * 
		 * @param value
		 *            The value to set the property to.
		 * @return The value of the property after being set.
		 * @throws NoUpdateException
		 *             If the user is not permitted to update the run.
		 * @throws NoListenerException
		 *             If the property is in the wrong format.
		 */
		@PUT
		@Path("/")
		@Consumes("text/plain")
		@Produces("text/plain")
		@Description("Set the value of the particular property of an event listener attached to a workflow run.")
		@NonNull
		String setValue(@NonNull String value) throws NoUpdateException,
				NoListenerException;

		/** Get an outline of the operations supported. */
		@OPTIONS
		@Path("/")
		@Description("Produces the description of one run listener's property's operations.")
		Response options();
	}

	/**
	 * A description of an event listener that is attached to a workflow run.
	 * Done with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "ListenerDescription")
	public class ListenerDescription extends VersionedElement {
		/** Where this listener is located. */
		@XmlAttribute(name = "href", namespace = XLINK)
		@XmlSchemaType(name = "anyURI")
		public URI location;
		/** The (arbitrary) name of the event listener. */
		@XmlAttribute
		public String name;
		/** The type of the event listener. */
		@XmlAttribute
		public String type;
		/**
		 * The location of the configuration document for the event listener.
		 */
		public Uri configuration;
		/**
		 * The name and location of the properties supported by the event
		 * listener.
		 */
		@XmlElementWrapper(name = "properties", nillable = false)
		@XmlElement(name = "property", nillable = false)
		public List<PropertyDescription> properties;

		/**
		 * Make a blank listener description.
		 */
		public ListenerDescription() {
		}

		/**
		 * Make a listener description that characterizes the given listener.
		 * 
		 * @param listener
		 *            The listener to describe.
		 * @param ub
		 *            The factory for URIs. Must have already been secured.
		 */
		public ListenerDescription(Listener listener, UriBuilder ub) {
			super(true);
			name = listener.getName();
			type = listener.getType();
			configuration = new Uri(ub.clone().path("configuration"));
			UriBuilder ub2 = ub.clone().path("properties/{prop}");
			properties = new ArrayList<PropertyDescription>(
					listener.listProperties().length);
			for (String propName : listener.listProperties())
				properties.add(new PropertyDescription(propName, ub2));
		}
	}

	/**
	 * The description of a single property, done with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlType(name = "PropertyDescription")
	public static class PropertyDescription extends Uri {
		/**
		 * The name of the property.
		 */
		@XmlAttribute
		String name;

		/**
		 * Make an empty description of a property.
		 */
		public PropertyDescription() {
		}

		/**
		 * Make a description of a property.
		 * 
		 * @param listenerName
		 *            The name of the listener whose property this is.
		 * @param propName
		 *            The name of the property.
		 * @param ub
		 *            The factory for URIs. Must have already been secured.
		 */
		PropertyDescription(String propName, UriBuilder ub) {
			super(ub, propName);
			this.name = propName;
		}
	}

	/**
	 * The list of descriptions of listeners attached to a run. Done with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class Listeners extends VersionedElement {
		/**
		 * The listeners for a workflow run.
		 */
		@XmlElement(name = "listener")
		public List<ListenerDescription> listener;

		/**
		 * Make a blank description of listeners.
		 */
		public Listeners() {
			listener = new ArrayList<ListenerDescription>();
		}

		/**
		 * Make a description of the whole group out of the given list of
		 * listener descriptions.
		 * 
		 * @param listeners
		 *            The collection of (partial) listener descriptions.
		 * @param ub
		 *            How to build the location of the listeners. Must have
		 *            already been secured.
		 */
		public Listeners(List<ListenerDescription> listeners, UriBuilder ub) {
			super(true);
			listener = listeners;
			for (ListenerDescription ld : listeners)
				ld.location = ub.build(ld.name);
		}
	}

	/**
	 * The list of properties of a listener. Done with JAXB.
	 * 
	 * @author Donal Fellows
	 */
	@XmlRootElement
	@XmlType(name = "")
	public static class Properties extends VersionedElement {
		/**
		 * The references to the properties of a listener.
		 */
		@XmlElement
		public List<PropertyDescription> property;

		/**
		 * Make an empty description of the properties of a listener.
		 */
		public Properties() {
		}

		/**
		 * Make the description of the properties of a listener.
		 * 
		 * @param ub
		 *            The factory for URIs, configured. Must have already been
		 *            secured.
		 * @param properties
		 *            The names of the properties.
		 */
		public Properties(UriBuilder ub, String[] properties) {
			super(true);
			property = new ArrayList<PropertyDescription>(properties.length);
			for (String propName : properties)
				property.add(new PropertyDescription(propName, ub));
		}
	}
}
