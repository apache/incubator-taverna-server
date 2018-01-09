/*
 */
package org.taverna.server.master;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static eu.medsea.util.MimeUtil.getMimeType;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.taverna.server.master.common.Uri.secure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.exceptions.FilesystemAccessException;
import org.taverna.server.master.exceptions.NoDirectoryEntryException;
import org.taverna.server.master.interfaces.Directory;
import org.taverna.server.master.interfaces.DirectoryEntry;
import org.taverna.server.master.interfaces.File;
import org.taverna.server.master.interfaces.TavernaRun;
import org.taverna.server.master.interfaces.UriBuilderFactory;
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

import org.apache.taverna.scufl2.api.container.WorkflowBundle;
import org.apache.taverna.scufl2.api.core.Workflow;
import org.apache.taverna.scufl2.api.port.InputWorkflowPort;
import org.apache.taverna.scufl2.api.port.OutputWorkflowPort;

/**
 * A class that is used to build descriptions of the contents of a workflow
 * run's filesystem.
 * 
 * @author Donal Fellows
 */
public class ContentsDescriptorBuilder {
	private Log log = getLog("Taverna.Server.Webapp");
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

	// -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

	private Workflow fillInFromWorkflow(TavernaRun run, UriBuilder ub,
			AbstractPortDescription portDesc) throws IOException {
		WorkflowBundle bundle = run.getWorkflow().getScufl2Workflow();
		bundle.getMainWorkflow().getInputPorts();
		portDesc.fillInBaseData(bundle.getMainWorkflow()
				.getIdentifier().toString(), run.getId(), ub.build());
		return bundle.getMainWorkflow();
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
			int mv = 1;
			for (AbstractValue v : ((ListValue) value).contents) {
				Integer d = computeDepth(v);
				if (d != null && mv <= d)
					mv = d + 1;
			}
			return mv;
		} else if (value instanceof LeafValue || value instanceof ErrorValue)
			return 0;
		else
			return null;
	}

	/**
	 * Build a description of a leaf value.
	 * 
	 * @param file
	 *            The file representing the value.
	 * @return A value descriptor.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	private LeafValue constructLeafValue(File file)
			throws FilesystemAccessException {
		LeafValue v = new LeafValue();
		v.fileName = file.getFullName();
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
	 * Build a description of an error value.
	 * 
	 * @param file
	 *            The file representing the error.
	 * @return A value descriptor.
	 * @throws FilesystemAccessException
	 *             If anything goes wrong.
	 */
	private ErrorValue constructErrorValue(File file)
			throws FilesystemAccessException {
		ErrorValue v = new ErrorValue();
		v.fileName = file.getFullName();
		v.byteLength = file.getSize();
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
		Set<DirectoryEntry> contents = new HashSet<>(dir.getContents());
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
			AbstractValue av;
			if (entry.getName().equals(error) && entry instanceof File) {
				av = constructErrorValue((File) entry);
			} else if (!entry.getName().equals(name)
					&& !entry.getName().startsWith(prefix))
				continue;
			else if (entry instanceof File)
				av = constructLeafValue((File) entry);
			else
				av = constructListValue((Directory) entry, ub);
			String fullPath = entry.getFullName().replaceFirst("^/", "");
			av.href = ub.clone().path(fullPath).build();
			return av;
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
			Workflow dataflow = fillInFromWorkflow(run, ub, descriptor);
			Collection<DirectoryEntry> outs = null;
			ub = ub.path("wd/{path}");
			for (OutputWorkflowPort output : dataflow.getOutputPorts()) {
				OutputPort p = descriptor.addPort(output.getName());
				if (run.getOutputBaclavaFile() == null) {
					if (outs == null)
						outs = fileUtils.getDirectory(run, "out").getContents();
					p.output = constructValue(outs, ub, p.name);
					p.depth = computeDepth(p.output);
				}
			}
		} catch (IOException e) {
			log.info("failure in conversion to .scufl2", e);
		}
		return descriptor;
	}

	private UriBuilder getRunUriBuilder(TavernaRun run, UriInfo ui) {
		if (ui == null)
			return secure(uriBuilderFactory.getRunUriBuilder(run));
		else
			return secure(fromUri(ui.getAbsolutePath().toString()
					.replaceAll("/(out|in)put/?$", "")));
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
			Workflow workflow = fillInFromWorkflow(run, ub, desc);
			ub = ub.path("input/{name}");
			for (InputWorkflowPort port : workflow.getInputPorts()) {
				InputPort in = desc.addPort(port.getName());
				in.href = ub.build(in.name);
				try {
					in.depth = port.getDepth();
				} catch (NumberFormatException ex) {
					in.depth = null;
				}
			}
		} catch (IOException e) {
			log.info("failure in conversion to .scufl2", e);
		}
		return desc;
	}
}
