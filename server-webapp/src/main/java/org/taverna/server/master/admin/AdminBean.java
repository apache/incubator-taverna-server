/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.admin;

import java.util.Arrays;

import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ManagementModel;
import org.taverna.server.master.localworker.IdAwareForkRunFactory;
import org.taverna.server.master.localworker.RunDBSupport;
import org.taverna.server.master.usage.UsageRecordRecorder;
import org.taverna.server.master.utils.InvocationCounter;

/**
 * The administration interface to Taverna Server.
 * 
 * @author Donal Fellows
 */
public class AdminBean implements Admin {
	@Required
	public void setState(ManagementModel state) {
		this.state = state;
	}

	@Required
	public void setCounter(InvocationCounter counter) {
		this.counter = counter;
	}

	@Required
	public void setRunDB(RunDBSupport runDB) {
		this.runDB = runDB;
	}

	@Required
	public void setFactory(IdAwareForkRunFactory factory) {
		this.factory = factory;
	}

	@Required
	public void setUsageRecords(UsageRecordRecorder usageRecords) {
		this.usageRecords = usageRecords;
	}

	ManagementModel state;
	private InvocationCounter counter;
	private RunDBSupport runDB;
	IdAwareForkRunFactory factory;
	private UsageRecordRecorder usageRecords;

	@Override
	public AdminDescription getDescription(UriInfo ui) {
		return new AdminDescription(ui);
	}

	@Override
	public BoolProperty allowNew() {
		return new AllowNewProperty(this);
	}

	@Override
	public BoolProperty logWorkflows() {
		return new LogWorkflowsProperty(this);
	}

	@Override
	public BoolProperty logFaults() {
		return new LogFaultsProperty(this);
	}

	@Override
	public StringProperty urFile() {
		return new URFileProperty(this);
	}

	@Override
	public int invokeCount() {
		return counter.getCount();
	}

	@Override
	public int runCount() {
		return runDB.countRuns();
	}

	@Override
	public StringProperty registryHost() {
		return new RegistryHostProperty(this);
	}

	@Override
	public IntegerProperty registryPort() {
		return new RegistryPortProperty(this);
	}

	@Override
	public IntegerProperty runLimit() {
		return new RunLimitProperty(this);
	}

	@Override
	public IntegerProperty defaultLifetime() {
		return new DefaultLifetimeProperty(this);
	}

	@Override
	public StringList currentRuns() {
		StringList result = new StringList();
		result.string = runDB.listRunNames();
		return result;
	}

	@Override
	public StringProperty javaBinary() {
		return new JavaBinaryProperty(this);
	}

	@Override
	public StringListProperty extraArguments() {
		return new ExtraArgsProperty(this);
	}

	@Override
	public StringProperty serverWorkerJar() {
		return new ServerWorkerProperty(this);
	}

	@Override
	public StringProperty executeWorkflowScript() {
		return new ExecuteWorkflowProperty(this);
	}

	@Override
	public IntegerProperty registrationWaitSeconds() {
		return new RegistrationWaitProperty(this);
	}

	@Override
	public IntegerProperty registrationPollMillis() {
		return new RegistrationPollProperty(this);
	}

	@Override
	public StringProperty runasPasswordFile() {
		return new RunAsPasswordFileProperty(this);
	}

	@Override
	public StringProperty serverForkerJar() {
		return new ServerForkerProperty(this);
	}

	@Override
	public int startupTime() {
		return factory.getLastStartupCheckCount();
	}

	@Override
	public Integer lastExitCode() {
		return factory.getLastExitCode();
	}

	@Override
	public StringList factoryProcessMapping() {
		StringList result = new StringList();
		result.string = Arrays.asList(factory.getFactoryProcessMapping());
		return result;
	}

	@Override
	public URList usageRecords() {
		URList result = new URList();
		result.usageRecord = usageRecords.getUsageRecords();
		return result;
	}

}
