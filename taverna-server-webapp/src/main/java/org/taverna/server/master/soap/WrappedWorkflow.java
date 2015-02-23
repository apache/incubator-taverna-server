/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.soap;

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

import org.taverna.server.master.common.Workflow;

import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;

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