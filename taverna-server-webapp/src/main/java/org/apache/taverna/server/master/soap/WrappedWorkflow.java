/*
 */
package org.apache.taverna.server.master.soap;
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

import static javax.xml.bind.annotation.XmlAccessType.NONE;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.taverna.server.master.common.Workflow;

import org.apache.taverna.scufl2.api.io.ReaderException;
import org.apache.taverna.scufl2.api.io.WorkflowBundleIO;
import org.apache.taverna.scufl2.api.io.WriterException;

/**
 * An MTOM-capable description of how to transfer the contents of a file.
 * 
 * @author Donal Fellows
 */
@XmlType(name = "WorkflowReference")
@XmlAccessorType(NONE)
public class WrappedWorkflow {
	@XmlMimeType("application/octet-stream")
	// JAXB bug: must be this
	public DataHandler workflowData;
	Workflow workflow;

	/**
	 * Initialize the contents of this descriptor from the given file and
	 * content type.
	 * 
	 * @param workflow
	 *            The workflow that is to be reported.
	 */
	public void setWorkflow(Workflow workflow) {
		workflowData = new DataHandler(new WorkflowSource(workflow));
	}

	@XmlTransient
	public Workflow getWorkflow() throws IOException {
		if (workflow != null)
			return workflow;
		try {
			return new Workflow(new WorkflowBundleIO().readBundle(
					workflowData.getInputStream(), null));
		} catch (ReaderException e) {
			throw new IOException("problem converting to scufl2 bundle", e);
		}
	}
}

/**
 * A data source that knows how to deliver a workflow.
 * 
 * @author Donal Fellows
 */
class WorkflowSource implements DataSource {
	WorkflowSource(Workflow workflow) {
		this.wf = workflow;
		this.io = new WorkflowBundleIO();
	}

	Workflow wf;
	final WorkflowBundleIO io;

	@Override
	public String getContentType() {
		return wf.getPreferredContentType().getContentType();
	}

	@Override
	public String getName() {
		switch (wf.getPreferredContentType()) {
		case SCUFL2:
			return "workflow.scufl2";
		case T2FLOW:
			return "workflow.t2flow";
		default:
			return "workflow";
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		PipedInputStream is = new PipedInputStream();
		final OutputStream os = new PipedOutputStream(is);
		new Worker() {
			@Override
			public void doWork() throws WriterException, IOException {
				io.writeBundle(wf.getScufl2Workflow(), os, wf
						.getPreferredContentType().getContentType());
			}

			@Override
			public void doneWork() {
				closeQuietly(os);
			}
		};
		return is;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		final PipedInputStream is = new PipedInputStream();
		OutputStream os = new PipedOutputStream(is);
		new Worker() {
			@Override
			public void doWork() throws IOException, ReaderException {
				wf = new Workflow(io.readBundle(is, null));
			}

			@Override
			public void doneWork() {
				closeQuietly(is);
			}
		};
		return os;
	}

	static abstract class Worker extends Thread {
		public Worker() {
			setDaemon(true);
			start();
		}

		public abstract void doWork() throws Exception;

		public abstract void doneWork();

		@Override
		public void run() {
			try {
				doWork();
			} catch (Exception e) {
				// do nothing.
			} finally {
				doneWork();
			}
		}
	}
}