/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master;

import static eu.medsea.util.MimeUtil.getMimeType;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static org.taverna.server.master.TavernaServerSupport.log;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.utils.FilenameUtils;
import org.taverna.server.port_description.AbsentValue;
import org.taverna.server.port_description.AbstractPortDescription;
import org.taverna.server.port_description.AbstractValue;
import org.taverna.server.port_description.ErrorValue;
import org.taverna.server.port_description.InputDescription;
import org.taverna.server.port_description.InputDescription.InputPort;
import org.taverna.server.port_description.LeafValue;
import org.taverna.server.port_description.ListValue;
import org.taverna.server.port_description.OutputDescription;
import org.taverna.server.port_description.OutputDescription.OutputPort;
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
	private XPathExpression inputPorts;
	private XPathExpression outputPortNames;
	private XPathExpression portName;
	private XPathExpression portDepth;
	private XPathExpression dataflow;

	public ContentsDescriptorBuilder() throws XPathExpressionException {
		XPath xp = XPathFactory.newInstance().newXPath();
		xp.setNamespaceContext(new NamespaceContext() {
			@Override
			public String getNamespaceURI(String prefix) {
				if (prefix.equals("t2flow"))
					return T2FLOW_NS;
				if (prefix.equals(XMLConstants.XML_NS_PREFIX))
					return XMLConstants.XML_NS_URI;
				if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE))
					return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
				return XMLConstants.NULL_NS_URI;
			}

			@Override
			public String getPrefix(String namespaceURI) {
				if (namespaceURI.equals(T2FLOW_NS))
					return "t2flow";
				if (namespaceURI.equals(XMLConstants.XML_NS_URI))
					return XMLConstants.XML_NS_PREFIX;
				if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI))
					return XMLConstants.XMLNS_ATTRIBUTE;
				return null;
			}

			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				return singletonList(getPrefix(namespaceURI)).iterator();
			}
		});
		dataflow = xp.compile("//t2flow:dataflow[1]");
		inputPorts = xp.compile("./t2flow:inputPorts/t2flow:port");
		outputPortNames = xp
				.compile("./t2flow:outputPorts/t2flow:port/t2flow:name");
		portName = xp.compile("./t2flow:name");
		portDepth = xp.compile("./t2flow:depth");
	}

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

	private Element fillInFromWorkflow(TavernaRun run, UriBuilder ub,
			AbstractPortDescription portDesc) throws XPathExpressionException {
		Element elem = run.getWorkflow().content[0];
		portDesc.fillInBaseData(elem.getAttribute("id"), run.getId(),
				ub.build());
		return (Element) dataflow.evaluate(elem, NODE);
	}

	/**
	 * Build the contents description.
	 * 
	 * @param run
	 *            The workflow run this is talking about.
	 * @param dataflow
	 *            The dataflow element of the T2flow document.
	 * @param ub
	 *            How to build URIs.
	 * @param descriptor
	 *            The descriptor to modify.
	 * @param expected
	 *            The list of outputs that are <i>expected</i> to be produced;
	 *            they might not actually produce anything though.
	 * @throws NoDirectoryEntryException
	 * @throws FilesystemAccessException
	 * @throws XPathExpressionException
	 */
	private void constructPorts(TavernaRun run, Element dataflow,
			UriBuilder ub, OutputDescription descriptor)
			throws FilesystemAccessException, NoDirectoryEntryException,
			XPathExpressionException {
		NodeList portNodeNames = (NodeList) outputPortNames.evaluate(dataflow,
				NODESET);
		Collection<DirectoryEntry> outs = fileUtils.getDirectory(run, "out")
				.getContents();
		for (int i = 0; i < portNodeNames.getLength(); i++) {
			OutputPort p = new OutputPort();
			p.name = portNodeNames.item(i).getTextContent();
			p.output = constructValue(outs, ub, p.name);
			p.depth = computeDepth(p.output);
			descriptor.ports.add(p);
		}
	}

	/**
	 * Computes the depth of value in a descriptor.
	 * 
	 * @param value
	 *            The value description to characterise.
	 * @return Its depth (i.e., the depth of the port outputting the value) or
	 *         <tt>null</tt> if that is impossible to determine.
	 */
	private Integer computeDepth(AbstractValue value) {
		if (value instanceof ListValue) {
			Integer mv = 1;
			for (AbstractValue v : ((ListValue) value).contents) {
				Integer d = computeDepth(v);
				if (d != null && mv <= d)
					mv = d + 1;
			}
			return mv;
		} else if (value instanceof LeafValue)
			return 0;
		else
			return null;
	}

	/**
	 * Build a description of a leaf value.
	 * 
	 * @param file
	 *            The file representing the value.
	 * @param ub
	 *            The factory for URIs.
	 * @return A value descriptor.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	private LeafValue constructLeafValue(File file, UriBuilder ub)
			throws FilesystemAccessException {
		LeafValue v = new LeafValue();
		v.fileName = file.getFullName();
		v.href = ub.build(file.getFullName().replaceFirst("^/", ""));
		v.byteLength = file.getSize();
		try {
			byte[] head = file.getContents(0, 1024);
			v.contentType = getMimeType(new ByteArrayInputStream(head));
		} catch (Exception e) {
			v.contentType = APPLICATION_OCTET_STREAM_TYPE.toString();
		}
		return v;
	}

	/**
	 * Build a description of a list value.
	 * 
	 * @param dir
	 *            The directory representing the list.
	 * @param ub
	 *            The factory for URIs.
	 * @return A value descriptor.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	private ListValue constructListValue(Directory dir, UriBuilder ub)
			throws FilesystemAccessException {
		ListValue v = new ListValue();
		v.length = 0;
		HashSet<DirectoryEntry> contents = new HashSet<DirectoryEntry>(
				dir.getContents());
		Iterator<DirectoryEntry> it = contents.iterator();
		while (it.hasNext())
			if (!it.next().getName().matches("^[0-9]+([.].*)?$"))
				it.remove();
		for (int i = 1; !contents.isEmpty(); i++) {
			String exact = Integer.toString(i);
			AbstractValue subval = constructValue(contents, ub, exact);
			v.contents.add(subval);
			if (!(subval instanceof AbsentValue)) {
				v.length = i;
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

	/**
	 * Build a value description.
	 * 
	 * @param parentContents
	 *            The contents of the parent directory.
	 * @param ub
	 *            The factory for URIs.
	 * @param name
	 *            The name of the value's file/directory representative.
	 * @return A value descriptor.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	private AbstractValue constructValue(
			Collection<DirectoryEntry> parentContents, UriBuilder ub,
			String name) throws FilesystemAccessException {
		String error = name + ".error";
		String prefix = name + ".";
		for (DirectoryEntry entry : parentContents) {
			if (entry.getName().equals(error)) {
				ErrorValue v = new ErrorValue();
				v.href = ub.build(entry.getFullName().replaceFirst("^/", ""));
				return v;
			}
			if (!entry.getName().equals(name)
					&& !entry.getName().startsWith(prefix))
				continue;
			if (entry instanceof File) {
				return constructLeafValue((File) entry, ub);
			} else {
				return constructListValue((Directory) entry, ub);
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
	 * @return The description, which can be serialized to XML.
	 * @throws FilesystemAccessException
	 *             If something goes wrong reading the directories.
	 * @throws NoDirectoryEntryException
	 *             If something goes wrong reading the directories.
	 */
	public OutputDescription makeOutputDescriptor(TavernaRun run, UriInfo ui)
			throws FilesystemAccessException, NoDirectoryEntryException {
		OutputDescription descriptor = new OutputDescription();
		try {
			UriBuilder ub = getRunUriBuilder(run, ui);
			Element dataflow = fillInFromWorkflow(run, ub, descriptor);
			if (dataflow == null || run.getOutputBaclavaFile() != null)
				return descriptor;
			constructPorts(run, dataflow, ub.path("wd/{path}"), descriptor);
		} catch (XPathExpressionException e) {
			log.info("failure in XPath evaluation", e);
		}
		return descriptor;
	}

	private UriBuilder getRunUriBuilder(TavernaRun run, UriInfo ui) {
		if (ui == null)
			return uriBuilderFactory.getRunUriBuilder(run);
		else
			return fromUri(ui.getAbsolutePath().toString()
					.replaceAll("/(out|in)put/?$", ""));
	}

	/**
	 * Constructs input descriptions.
	 * 
	 * @param run
	 *            The run to build for.
	 * @param ui
	 *            The mechanism for building URIs.
	 * @return The description of the <i>expected</i> inputs of the run.
	 */
	public InputDescription makeInputDescriptor(TavernaRun run, UriInfo ui) {
		InputDescription desc = new InputDescription();
		try {
			UriBuilder ub = getRunUriBuilder(run, ui);
			Element elem = fillInFromWorkflow(run, ub, desc);
			ub = ub.path("input/{name}");
			if (elem == null)
				return desc; // Not t2flow
			// Foreach "./inputPorts/port"
			NodeList nl = (NodeList) inputPorts.evaluate(elem, NODESET);
			for (int i = 0; i < nl.getLength(); i++) {
				InputPort in = new InputPort();
				in.name = portName.evaluate(nl.item(i)); // "./name"
				in.href = ub.build(in.name);
				try {
					in.depth = Integer.valueOf(portDepth.evaluate(nl.item(i))); // "./depth"
				} catch (NumberFormatException ex) {
					in.depth = null;
				} catch (DOMException ex) {
					in.depth = null;
				}
				desc.input.add(in);
			}
		} catch (XPathExpressionException e) {
			log.info("failure in XPath evaluation", e);
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