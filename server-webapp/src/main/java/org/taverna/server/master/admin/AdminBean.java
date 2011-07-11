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
	public boolean getAllowNew() {
		return state.getAllowNewWorkflowRuns();
	}

	@RolesAllowed(ADMIN)
	@Override
	public boolean setAllowNew(boolean newValue) {
		state.setAllowNewWorkflowRuns(newValue);
		return state.getAllowNewWorkflowRuns();
	}

	@RolesAllowed(ADMIN)
	@Override
	public boolean getLogWorkflows() {
		return state.getLogIncomingWorkflows();
	}

	@RolesAllowed(ADMIN)
	@Override
	public boolean setLogWorkflows(boolean newValue) {
		state.setLogIncomingWorkflows(newValue);
		return state.getLogIncomingWorkflows();
	}

	@RolesAllowed(ADMIN)
	@Override
	public boolean getLogFaults() {
		return state.getLogOutgoingExceptions();
	}

	@RolesAllowed(ADMIN)
	@Override
	public boolean setLogFaults(boolean newValue) {
		state.setLogOutgoingExceptions(newValue);
		return state.getLogOutgoingExceptions();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String getURFile() {
		return state.getUsageRecordLogFile();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setURFile(String newValue) {
		state.setUsageRecordLogFile(newValue);
		return state.getUsageRecordLogFile();
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
	public String getRegistryHost() {
		return factory.getRegistryHost();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setRegistryHost(String newValue) {
		factory.setRegistryHost(newValue);
		return factory.getRegistryHost();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int getRegistryPort() {
		return factory.getRegistryPort();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int setRegistryPort(int newValue) {
		factory.setRegistryPort(newValue);
		return factory.getRegistryPort();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int getRunLimit() {
		return factory.getMaxRuns();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int setRunLimit(int newValue) {
		factory.setMaxRuns(newValue);
		return factory.getMaxRuns();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int getDefaultLifetime() {
		return factory.getDefaultLifetime();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int setDefaultLifetime(int newValue) {
		factory.setDefaultLifetime(newValue);
		return factory.getDefaultLifetime();
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
	public String getJavaBinary() {
		return factory.getJavaBinary();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setJavaBinary(String newValue) {
		factory.setJavaBinary(newValue);
		return factory.getJavaBinary();
	}

	@RolesAllowed(ADMIN)
	@Override
	public StringList getExtraArguments() {
		StringList result = new StringList();
		result.string = Arrays.asList(factory.getExtraArguments());
		return result;
	}
	@RolesAllowed(ADMIN)
	@Override
	public StringList setExtraArguments(StringList newValue) {
		factory.setExtraArguments(newValue.string
				.toArray(new String[newValue.string.size()]));
		StringList result = new StringList();
		result.string = Arrays.asList(factory.getExtraArguments());
		return result;
	}

	@RolesAllowed(ADMIN)
	@Override
	public String getServerWorkerJar() {
		return factory.getServerWorkerJar();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setServerWorkerJar(String newValue) {
		factory.setServerWorkerJar(newValue);
		return factory.getServerWorkerJar();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String getExecuteWorkflowScript() {
		return factory.getExecuteWorkflowScript();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setExecuteWorkflowScript(String newValue) {
		factory.setExecuteWorkflowScript(newValue);
		return factory.getExecuteWorkflowScript();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int getRegistrationWaitSeconds() {
		return factory.getWaitSeconds();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int setRegistrationWaitSeconds(int newValue) {
		factory.setWaitSeconds(newValue);
		return factory.getWaitSeconds();
	}

	@RolesAllowed(ADMIN)
	@Override
	public int getRegistrationPollMillis() {
		return factory.getSleepTime();
	}
	@RolesAllowed(ADMIN)
	@Override
	public int setRegistrationPollMillis(int newValue) {
		factory.setSleepTime(newValue);
		return factory.getSleepTime();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String getRunasPasswordFile() {
		return factory.getPasswordFile();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setRunasPasswordFile(String newValue) {
		factory.setPasswordFile(newValue);
		return factory.getPasswordFile();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String getServerForkerJar() {
		return factory.getServerForkerJar();
	}

	@RolesAllowed(ADMIN)
	@Override
	public String setServerForkerJar(String newValue) {
		factory.setServerForkerJar(newValue);
		return factory.getServerForkerJar();
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
