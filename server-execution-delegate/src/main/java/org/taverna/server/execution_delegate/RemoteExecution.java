package org.taverna.server.execution_delegate;

import static javax.xml.bind.annotation.XmlAccessType.NONE;

import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Interface for a single execution of a Taverna workflow in a remote process.
 * 
 * @author David Withers
 */
public interface RemoteExecution extends Remote {

	/**
	 * Returns the identifier for this execution.
	 * 
	 * @return the identifier for this execution
	 */
	String getID() throws RemoteException;

	/**
	 * Returns the <code>WorkflowReport</code> for the execution.
	 * 
	 * @return the <code>WorkflowReport</code> for the execution
	 */
	WorkflowReportDocument getWorkflowReport() throws RemoteException;

	/**
	 * Deletes the execution.
	 */
	void delete() throws RemoteException;

	/**
	 * Starts the execution.
	 */
	void start() throws RemoteException;

	/**
	 * Pauses the execution.
	 */
	void pause() throws RemoteException;

	/**
	 * Resumes a paused execution.
	 */
	void resume() throws RemoteException;

	/**
	 * Cancels the execution.
	 */
	void cancel() throws RemoteException;

	@XmlType(name = "Report", propOrder = { "state", "created", "started",
			"completed", "failed", "cancelled", "paused", "resumed" })
	@XmlSeeAlso({ WorkflowReportDocument.class, ProcessorReportDocument.class,
			ActivityReportDocument.class })
	@XmlAccessorType(NONE)
	public static abstract class ReportDocument {
		@XmlAttribute
		public String subject;
		@XmlElement
		public String state;
		@XmlElement(required = true)
		@XmlSchemaType(name = "dateTime")
		public XMLGregorianCalendar created;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public XMLGregorianCalendar started;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public XMLGregorianCalendar completed;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public XMLGregorianCalendar failed;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public XMLGregorianCalendar cancelled;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public List<XMLGregorianCalendar> paused;
		@XmlElement
		@XmlSchemaType(name = "dateTime")
		public List<XMLGregorianCalendar> resumed;
	}

	@XmlType(name = "WorkflowReport", propOrder = { "processor", "inputs",
			"outputs" })
	@XmlRootElement(name = "workflowReport")
	@XmlAccessorType(NONE)
	public static class WorkflowReportDocument extends ReportDocument {
		@XmlElement(name = "processor")
		public ArrayList<ProcessorReportDocument> processor = new ArrayList<ProcessorReportDocument>();
		@XmlElement(name = "input")
		public ArrayList<PortMapping> inputs = new ArrayList<PortMapping>();
		@XmlElement(name = "output")
		public ArrayList<PortMapping> outputs = new ArrayList<PortMapping>();
	}

	@XmlType(name = "ProcessorReport", propOrder = { "jobsQueued",
			"jobsStarted", "jobsCompleted", "jobsErrored", "properties",
			"activity" })
	@XmlAccessorType(NONE)
	public static class ProcessorReportDocument extends ReportDocument {
		@XmlElement(name = "jobsQueued", required = true)
		public int jobsQueued;
		@XmlElement(name = "jobsStarted", required = true)
		public int jobsStarted;
		@XmlElement(name = "jobsCompleted", required = true)
		public int jobsCompleted;
		@XmlElement(name = "jobsErrored", required = true)
		public int jobsErrored;
		@XmlElement(name = "property")
		@XmlElementWrapper(name = "properties")
		public ArrayList<Property> properties = new ArrayList<Property>();
		@XmlElement(name = "activity")
		public ArrayList<ActivityReportDocument> activity = new ArrayList<ActivityReportDocument>();

		@XmlType(name = "ProcessorProperty")
		@XmlAccessorType(NONE)
		public static class Property {
			@XmlAttribute(name = "key", required = true)
			String key;
			@XmlValue
			String value;

			public Property() {
			}

			public String getKey() {
				return key;
			}

			public String getValue() {
				return value;
			}

			Property(String key, String value) {
				this.key = key;
				this.value = value;
			}
		}
	}

	@XmlType(name = "ActivityReport", propOrder = { "workflow", "inputs",
			"outputs" })
	@XmlAccessorType(NONE)
	public static class ActivityReportDocument extends ReportDocument {
		@XmlElement(name = "workflow")
		public ArrayList<WorkflowReportDocument> workflow = new ArrayList<WorkflowReportDocument>();
		@XmlElement(name = "input")
		public ArrayList<PortMapping> inputs = new ArrayList<PortMapping>();
		@XmlElement(name = "output")
		public ArrayList<PortMapping> outputs = new ArrayList<PortMapping>();
	}

	@XmlType(name = "PortMapping")
	@XmlAccessorType(NONE)
	public static class PortMapping {
		public PortMapping() {
		}

		PortMapping(String port, URI data) {
			this.name = port;
			this.reference = data;
		}

		@XmlAttribute(name = "name")
		public String name;
		@XmlAttribute(name = "ref")
		@XmlSchemaType(name = "anyURI")
		public URI reference;
	}
}
