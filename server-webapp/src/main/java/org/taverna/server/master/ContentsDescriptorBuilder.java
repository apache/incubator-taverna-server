/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static eu.medsea.util.MimeUtil.getMimeType;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.UriBuilder.fromUri;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.input_description.Input;
import org.taverna.server.input_description.InputDescription;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.output_description.AbsentValue;
import org.taverna.server.output_description.AbstractValue;
import org.taverna.server.output_description.ErrorValue;
import org.taverna.server.output_description.LeafValue;
import org.taverna.server.output_description.ListValue;
import org.taverna.server.output_description.Outputs;
import org.taverna.server.output_description.Outputs.Port;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A class that is used to build descriptions of the contents of a workflow
 * run's filesystem.
 * 
 * @author Donal Fellows
 */
public class ContentsDescriptorBuilder {
	private FilenameUtils fileUtils;
	private UriBuilderFactory uriBuilderFactory;

	@Required
	public void setUriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
	}

	@Required
	public void setFileUtils(FilenameUtils fileUtils) {
		this.fileUtils = fileUtils;
	}

	/** Namespace for use when pulling apart a .t2flow document. */
	private static final String T2FLOW_NS = "http://taverna.sf.net/2008/xml/t2flow";
	/** Where RDF names are rooted. */
	private static final String RDF_BASE = "http://ns.taverna.org.uk/2010/run/";

	/**
	 * Build the contents description.
	 * 
	 * @param run
	 *            The workflow run this is talking about.
	 * @param ub
	 *            How to build URIs.
	 * @param descriptor
	 *            The descriptor to modify.
	 * @param expected
	 *            The list of outputs that are <i>expected</i> to be produced;
	 *            they might not actually produce anything though.
	 * @throws NoDirectoryEntryException
	 * @throws FilesystemAccessException
	 */
	private void constructPorts(TavernaRun run, UriBuilder ub,
			Outputs descriptor) throws FilesystemAccessException,
			NoDirectoryEntryException {
		NodeList nl, nl2;
		Element e = run.getWorkflow().content[0];
		nl = e.getElementsByTagNameNS(T2FLOW_NS, "dataflow");
		if (nl.getLength() == 0)
			return; // Not t2flow
		nl = ((Element) nl.item(0)).getElementsByTagNameNS(T2FLOW_NS,
				"outputPorts");
		if (nl.getLength() == 0)
			return; // No outputs
		nl = ((Element) nl.item(0)).getElementsByTagNameNS(T2FLOW_NS, "port");
		Collection<DirectoryEntry> outs = fileUtils.getDirectory(run, "out")
				.getContents();
		for (int i = 0; i < nl.getLength(); i++) {
			nl2 = ((Element) nl.item(i)).getElementsByTagNameNS(T2FLOW_NS,
					"name");
			if (nl2.getLength() == 1) {
				Port p = new Port();
				p.name = nl2.item(0).getTextContent();
				p.output = constructValue(outs, ub, p.name);
				descriptor.ports.add(p);
			}
		}
	}

	private AbstractValue constructValue(
			Collection<DirectoryEntry> parentContents, UriBuilder ub,
			String name) throws FilesystemAccessException {
		String error = name + ".err";
		String prefix = name + ".";
		for (DirectoryEntry entry : parentContents) {
			if (entry.getName().equals(error)) {
				ErrorValue v = new ErrorValue();
				v.href = ub.build(entry.getFullName());
				return v;
			}
			if (!entry.getName().equals(name)
					&& !entry.getName().startsWith(prefix))
				continue;
			if (entry instanceof File) {
				File f = (File) entry;
				LeafValue v = new LeafValue();
				v.href = ub.build(f.getFullName());
				v.byteLength = f.getSize();
				try {
					byte[] head = f.getContents(0, 1024);
					v.contentType = getMimeType(new ByteArrayInputStream(head));
				} catch (Exception e) {
					v.contentType = APPLICATION_OCTET_STREAM_TYPE.toString();
				}
				return v;
			} else {
				ListValue v = new ListValue();
				Directory dir = (Directory) entry;
				HashSet<DirectoryEntry> contents = new HashSet<DirectoryEntry>(
						dir.getContents());
				Iterator<DirectoryEntry> it = contents.iterator();
				while (it.hasNext())
					if (!it.next().getName().matches("^[0-9]+([.].*)$"))
						it.remove();
				for (int i = 0; !contents.isEmpty(); i++) {
					v.length = i;
					String exact = Integer.toString(i);
					AbstractValue subval = constructValue(contents, ub, exact);
					v.contents.add(subval);
					if (!(subval instanceof AbsentValue)) {
						String pfx = i + ".";
						for (DirectoryEntry de : contents)
							if (de.getName().equals(exact)
									|| de.getName().startsWith(pfx)) {
								contents.remove(de);
								break;
							}
					}
				}
				return v;
			}
		}
		return new AbsentValue();
	}

	/**
	 * Construct a description of the outputs of a workflow run.
	 * 
	 * @param run
	 *            The workflow run whose outputs are to be described.
	 * @param ui
	 *            The origin for URIs.
	 * @return The description, which can be serialized to RDF+XML.
	 * @throws FilesystemAccessException
	 *             If something goes wrong reading the directories.
	 * @throws NoDirectoryEntryException
	 *             If something goes wrong reading the directories.
	 */
	public Outputs makeOutputDescriptor(TavernaRun run, UriInfo ui)
			throws FilesystemAccessException, NoDirectoryEntryException {
		Outputs descriptor = new Outputs();
		// RdfWrapper descriptor = new RdfWrapper();
		UriBuilder ub;
		if (ui == null)
			ub = uriBuilderFactory.getRunUriBuilder(run);
		else
			ub = fromUri(ui.getAbsolutePathBuilder().path("..").build());
		descriptor.workflowRun = ub.build();
		try {
			Element elem = run.getWorkflow().content[0];
			NodeList nl = elem.getElementsByTagNameNS(T2FLOW_NS, "dataflow");
			if (nl.getLength() > 0) {
				elem = (Element) nl.item(0);
				if (elem.hasAttribute("id"))
					descriptor.workflowId = elem.getAttribute("id");
			}
		} catch (Exception e) {
			// Ignore
		}
		if (run.getOutputBaclavaFile() != null) {
			return descriptor;
		}

		// ArrayList<String> expected = new ArrayList<String>();
		constructPorts(run, ub.clone().path("wd/{path}"), descriptor);
		return descriptor;
	}

	/**
	 * Constructs input descriptions.
	 * 
	 * @param run
	 *            The run to build for.
	 * @param ub
	 *            The mechanism for building URIs.
	 * @return The description of the <i>expected</i> inputs of the run.
	 */
	public InputDescription makeInputDescriptor(TavernaRun run, UriBuilder ub) {
		NodeList nl;
		InputDescription desc = new InputDescription();
		ub = ub.path("{name}");
		desc.input = new ArrayList<Input>();
		try {
			Element elem = run.getWorkflow().content[0];
			nl = elem.getElementsByTagNameNS(T2FLOW_NS, "dataflow");
			if (nl.getLength() == 0)
				return desc; // Not t2flow
			elem = (Element) nl.item(0);
			if (elem.hasAttribute("id"))
				desc.about = RDF_BASE + elem.getAttribute("id");

			// Foreach "./inputPorts/port"
			nl = elem.getElementsByTagNameNS(T2FLOW_NS, "inputPorts");
			if (nl.getLength() == 0)
				return desc; // Not t2flow or no inputs
			elem = (Element) nl.item(0);
			nl = elem.getElementsByTagNameNS(T2FLOW_NS, "port");
			for (int i = 0; i < nl.getLength(); i++) {
				Input in = new Input();
				elem = (Element) nl.item(i);
				NodeList names = elem.getElementsByTagNameNS(T2FLOW_NS, "name");
				in.name = names.item(0).getTextContent();
				in.href = ub.build(in.name);
				if (desc.about != null)
					in.about = desc.about + "/input/" + in.name;
				try {
					NodeList depths = elem.getElementsByTagNameNS(T2FLOW_NS,
							"depth");
					in.depth = Integer.valueOf(depths.item(0).getTextContent(),
							10);
				} catch (NumberFormatException ex) {
					in.depth = null;
				} catch (DOMException ex) {
					in.depth = null;
				}
				desc.input.add(in);
			}
		} catch (DOMException ex) {
			// Ignore this exception; just results in failure to fill out desc
		}
		return desc;
	}

	/**
	 * How to manufacture URIs to workflow runs.
	 * 
	 * @author Donal Fellows
	 */
	public interface UriBuilderFactory {
		/**
		 * Given a run, get a factory for RESTful URIs to resources associated
		 * with it.
		 * 
		 * @param run
		 *            The run in question.
		 * @return The {@link URI} factory.
		 */
		UriBuilder getRunUriBuilder(TavernaRun run);

		/**
		 * @return a URI factory that is preconfigured to point to the base of
		 *         the webapp.
		 */
		UriBuilder getBaseUriBuilder();
	}
}