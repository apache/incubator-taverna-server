/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.admin;

import static org.taverna.server.master.common.Roles.ADMIN;

import java.util.Arrays;

import javax.annotation.security.RolesAllowed;
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

	@RolesAllowed(ADMIN)
	@Override
	public AdminDescription getDescription(UriInfo ui) {
		return new AdminDescription(ui);
	}

	@RolesAllowed(ADMIN)
	@Override
	public BoolProperty allowNew() {
		return new AllowNewProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public BoolProperty logWorkflows() {
		return new LogWorkflowsProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public BoolProperty logFaults() {
		return new LogFaultsProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty urFile() {
		return new URFileProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public int invokeCount() {
		return counter.getCount();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int runCount() {
		return runDB.countRuns();
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty registryHost() {
		return new RegistryHostProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public IntegerProperty registryPort() {
		return new RegistryPortProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public IntegerProperty runLimit() {
		return new RunLimitProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public IntegerProperty defaultLifetime() {
		return new DefaultLifetimeProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringList currentRuns() {
		StringList result = new StringList();
		result.string = runDB.listRunNames();
		return result;
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty javaBinary() {
		return new JavaBinaryProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringListProperty extraArguments() {
		return new ExtraArgsProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty serverWorkerJar() {
		return new ServerWorkerProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty executeWorkflowScript() {
		return new ExecuteWorkflowProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public IntegerProperty registrationWaitSeconds() {
		return new RegistrationWaitProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public IntegerProperty registrationPollMillis() {
		return new RegistrationPollProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty runasPasswordFile() {
		return new RunAsPasswordFileProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringProperty serverForkerJar() {
		return new ServerForkerProperty(this);
	}

	@RolesAllowed(ADMIN)
	@Override
	public int startupTime() {
		return factory.getLastStartupCheckCount();
	}

	@RolesAllowed(ADMIN)
	@Override
	public Integer lastExitCode() {
		return factory.getLastExitCode();
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringList factoryProcessMapping() {
		StringList result = new StringList();
		result.string = Arrays.asList(factory.getFactoryProcessMapping());
		return result;
	}

	@RolesAllowed(ADMIN)
	@Override
	public URList usageRecords() {
		URList result = new URList();
		result.usageRecord = usageRecords.getUsageRecords();
		return result;
	}

}
