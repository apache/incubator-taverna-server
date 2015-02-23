package org.taverna.server.execution_delegate;

import java.net.URI;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.taverna.server.execution_delegate.RemoteExecution.ProcessorReportDocument.Property;

import uk.org.taverna.platform.data.api.Data;
import uk.org.taverna.platform.data.api.DataLocation;
import uk.org.taverna.platform.execution.api.Execution;
import uk.org.taverna.platform.report.ActivityReport;
import uk.org.taverna.platform.report.ProcessorReport;
import uk.org.taverna.platform.report.StatusReport;
import uk.org.taverna.platform.report.WorkflowReport;
import uk.org.taverna.scufl2.api.common.AbstractNamed;

public class ExecutionDelegate extends UnicastRemoteObject implements
		RemoteExecution {
	private Execution delegated;
	private DatatypeFactory dtf;

	public ExecutionDelegate(Execution execution) throws RemoteException,
			DatatypeConfigurationException {
		super();
		delegated = execution;
		dtf = DatatypeFactory.newInstance();
	}

	@Override
	public String getID() {
		return delegated.getID();
	}

	@Override
	public void delete() {
		delegated.delete();
	}

	@Override
	public void start() {
		delegated.start();
	}

	@Override
	public void pause() {
		delegated.pause();
	}

	@Override
	public void resume() {
		delegated.resume();
	}

	@Override
	public void cancel() {
		delegated.cancel();
	}

	private XMLGregorianCalendar date(Date d) {
		if (d == null)
			return null;
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(d);
		return dtf.newXMLGregorianCalendar(c);
	}

	private <R extends ReportDocument, T extends AbstractNamed> R init(
			R snapshot, StatusReport<T, ?, ?> report) {
		snapshot.created = date(report.getCreatedDate());
		snapshot.completed = date(report.getCompletedDate());
		snapshot.cancelled = date(report.getCancelledDate());
		snapshot.failed = date(report.getFailedDate());
		snapshot.started = date(report.getStartedDate());
		for (Date d : report.getPausedDates())
			snapshot.paused.add(date(d));
		for (Date d : report.getResumedDates())
			snapshot.resumed.add(date(d));
		snapshot.subject = report.getSubject().getName();
		snapshot.state = report.getState().toString();
		return snapshot;
	}

	private WorkflowReportDocument getReport(WorkflowReport report) {
		WorkflowReportDocument snapshot = init(new WorkflowReportDocument(),
				report);
		for (ProcessorReport pr : report.getChildReports())
			snapshot.processor.add(getReport(pr));
		initMap(snapshot.inputs, report.getInputs());
		initMap(snapshot.outputs, report.getOutputs());
		return snapshot;
	}

	private void initMap(ArrayList<PortMapping> snapshot, Map<String, ?> report) {
		for (String port : report.keySet())
			snapshot.add(data(port, report.get(port)));
	}

	private static final String DEFAULT_PROPERTY_STRING = "";

	private PortMapping data(String name, Object o) {
		String loc;
		if (o instanceof DataLocation) {
			DataLocation d = (DataLocation) o;
			loc = d.getDataServiceURI() + "#" + d.getDataID();
		} else {
			Data d = (Data) o;
			loc = null;
			if (d.hasReferences()) {
				try {
					loc = d.getReferences().iterator().next().getURI()
							.toString();
				} catch (Exception e) {
				}
			}
			if (loc == null) {
				DataLocation dl = d.getDataService().getDataLocation(d);
				loc = dl.getDataServiceURI() + "#" + dl.getDataID();
			}
		}
		return new PortMapping(name, URI.create(loc));
	}

	private ProcessorReportDocument getReport(ProcessorReport report) {
		ProcessorReportDocument snapshot = init(new ProcessorReportDocument(),
				report);
		for (ActivityReport pr : report.getChildReports())
			snapshot.activity.add(getReport(pr));
		snapshot.jobsQueued = report.getJobsStarted();
		snapshot.jobsStarted = report.getJobsStarted();
		snapshot.jobsCompleted = report.getJobsCompleted();
		snapshot.jobsErrored = report.getJobsCompletedWithErrors();
		for (String key : report.getPropertyKeys()) {
			Object value = report.getProperty(key);
			if (value instanceof String || value instanceof Number)
				snapshot.properties.add(new Property(key, value.toString()));
			else
				snapshot.properties.add(new Property(key,
						DEFAULT_PROPERTY_STRING));
		}
		return snapshot;
	}

	private ActivityReportDocument getReport(ActivityReport report) {
		ActivityReportDocument snapshot = init(new ActivityReportDocument(),
				report);
		for (WorkflowReport pr : report.getChildReports())
			snapshot.workflow.add(getReport(pr));
		initMap(snapshot.inputs, report.getInputs());
		initMap(snapshot.outputs, report.getOutputs());
		return snapshot;
	}

	@Override
	public WorkflowReportDocument getWorkflowReport() {
		return getReport(delegated.getWorkflowReport());
	}
}

// ExecutionDelegate.java:[96,2]
// initMap(java.util.ArrayList<org.taverna.server.execution_delegate.RemoteExecution.PortMapping>,java.util.Map<java.lang.String,uk.org.taverna.platform.data.api.Data>)
// in org.taverna.server.execution_delegate.ExecutionDelegate cannot be applied
// to
// (java.util.ArrayList<org.taverna.server.execution_delegate.RemoteExecution.PortMapping>,java.util.Map<java.lang.String,uk.org.taverna.platform.data.api.DataLocation>)
