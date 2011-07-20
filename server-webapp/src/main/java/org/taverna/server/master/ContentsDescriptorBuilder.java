/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static eu.medsea.util.MimeUtil.getMimeType;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.UriBuilder.fromUri;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.taverna.server.output_description.Outputs.Contains;
import org.taverna.server.output_description.RdfWrapper;
import org.taverna.server.output_description.RdfWrapper.Run;
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
	 */
	private void constructContents(TavernaRun run, UriBuilder ub,
			RdfWrapper descriptor, ArrayList<String> expected) {
		NodeList nl, nl2;
		Element e = run.getWorkflow().content[0];
		nl = e.getElementsByTagNameNS(T2FLOW_NS, "dataflow");
		if (nl.getLength() == 0)
			return; // Not t2flow
		String id = ((Element) nl.item(0)).getAttribute("id");
		if (id != null && !id.isEmpty())
			descriptor.run.about = RDF_BASE + id;
		nl = ((Element) nl.item(0)).getElementsByTagNameNS(T2FLOW_NS,
				"outputPorts");
		if (nl.getLength() == 0)
			return; // No outputs
		nl = ((Element) nl.item(0)).getElementsByTagNameNS(T2FLOW_NS, "port");
		for (int i = 0; i < nl.getLength(); i++) {
			nl2 = ((Element) nl.item(i)).getElementsByTagNameNS(T2FLOW_NS,
					"name");
			if (nl2.getLength() == 1) {
				Contains c = new Contains();
				c.resource = "out/" + nl2.item(0).getTextContent();
				descriptor.outputsDescription.contains.add(c);
			}
		}
	}

	/**
	 * Fills in attributes specific to a leaf value.
	 * 
	 * @param f
	 *            The file which is the source of the information.
	 * @param lv
	 *            The value that is to be updated.
	 */
	private void fillInLeafValue(File f, LeafValue lv) {
		try {
			lv.byteLength = f.getSize();
		} catch (FilesystemAccessException e) {
			// Ignore exception; will result in omitted attribute
		}
		try {
			byte[] head = f.getContents(0, 1024);
			lv.contentType = getMimeType(new ByteArrayInputStream(head));
		} catch (Exception e) {
			lv.contentType = APPLICATION_OCTET_STREAM_TYPE.toString();
		}
	}

	/**
	 * Fill in attributes and contents specific to a list value.
	 * 
	 * @param d
	 *            The directory which is the source of the information.
	 * @param baseURI
	 *            The base URI of the directory.
	 * @param lv
	 *            The list value to be built.
	 */
	private void fillInListValue(Directory d, URI baseURI, ListValue lv) {
		Map<Integer, DirectoryEntry> numbered = new HashMap<Integer, DirectoryEntry>();
		Set<Integer> errors = new HashSet<Integer>();
		try {
			for (DirectoryEntry de : d.getContents()) {
				String name = de.getName();
				try {
					if (name.endsWith(".err")) {
						name = name.substring(0, name.length() - 4);
						int i = parseInt(name);
						if (i >= 0)
							errors.add(i);
					} else {
						int i = parseInt(name);
						if (i >= 0)
							numbered.put(i, de);
					}
				} catch (NumberFormatException nfe) {
					// skip
					break;
				}
			}
		} catch (FilesystemAccessException e) {
			// Couldn't list the directory contents;
			// We model this as an empty directory.
		}
		for (int i = 0; !(numbered.isEmpty() && errors.isEmpty()); i++) {
			AbstractValue av;
			DirectoryEntry de = numbered.remove(i);
			if (de != null) {
				if (de instanceof Directory) {
					av = new ListValue();
					fillInListValue(
							(Directory) de,
							fromUri(baseURI).path("{dir}").build(
									Integer.toString(i)), (ListValue) av);
				} else {
					av = new LeafValue();
					fillInLeafValue((File) de, (LeafValue) av);
				}
				av.setAddress(baseURI, Integer.toString(i));
			} else if (errors.remove(i)) {
				av = new ErrorValue();
				av.setAddress(baseURI, i + ".err");
			} else {
				av = new AbsentValue();
				av.setAddress(baseURI, Integer.toString(i));
			}
			lv.contents.add(av);
		}
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
	public RdfWrapper makeOutputDescriptor(TavernaRun run, UriInfo ui)
			throws FilesystemAccessException, NoDirectoryEntryException {
		RdfWrapper descriptor = new RdfWrapper();
		UriBuilder ub;
		if (ui == null)
			ub = uriBuilderFactory.getRunUriBuilder(run);
		else
			ub = fromUri(ui.getAbsolutePathBuilder().path("..").build());
		descriptor.run.href = ub.build();
		descriptor.run.runid.resource = run.getId();
		if (run.getOutputBaclavaFile() != null) {
			return descriptor;
		}

		ArrayList<String> expected = new ArrayList<String>();
		constructContents(run, ub, descriptor, expected);
		Directory out = (Directory) fileUtils.getDirEntry(run, "out");
		URI outUri = ub.path("out").build();

		for (String outputName : expected) {
			String eName = outputName + ".err";
			AbstractValue v = new AbsentValue();
			for (DirectoryEntry de : out.getContents()) {
				if (outputName.equals(de.getName())) {
					if (de instanceof Directory) {
						v = new ListValue();
						fillInListValue((Directory) de, outUri, (ListValue) v);
						break;
					} else if (de instanceof File) {
						v = new LeafValue();
						fillInLeafValue((File) de, (LeafValue) v);
						break;
					} else {
						continue;
					}
				} else if (eName.equals(de.getName())) {
					v = new ErrorValue();
					break;
				}
			}
			v.setAddress(outUri, outputName);
			v.output = outputName;
			descriptor.outputs.add(v);
		}
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