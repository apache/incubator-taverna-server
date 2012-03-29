/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.admin;

import static org.taverna.server.master.common.Roles.ADMIN;

import java.util.Arrays;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Required;
import org.taverna.server.master.ManagementModel;
import org.taverna.server.master.factories.ConfigurableRunFactory;
import org.taverna.server.master.identity.User;
import org.taverna.server.master.identity.UserStore;
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
	public void setFactory(ConfigurableRunFactory factory) {
		this.factory = factory;
	}

	@Required
	public void setUsageRecords(UsageRecordRecorder usageRecords) {
		this.usageRecords = usageRecords;
	}

	@Required
	public void setUserStore(UserStore userStore) {
		this.userStore = userStore;
	}

	private ManagementModel state;
	private InvocationCounter counter;
	private RunDBSupport runDB;
	private ConfigurableRunFactory factory;
	private UsageRecordRecorder usageRecords;
	private UserStore userStore;

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
		String[] xargs = factory.getExtraArguments();
		StringList result = new StringList();
		result.string = Arrays.asList(xargs == null ? new String[0] : xargs);
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

	@RolesAllowed(ADMIN)
	@Override
	public UserList users(UriInfo ui) {
		UserList ul = new UserList();
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{id}");
		for (String user : userStore.getUserNames())
			ul.user.add(ub.build(user));
		return ul;
	}

	@RolesAllowed(ADMIN)
	@Override
	public UserDesc user(String username) {
		UserDesc desc = new UserDesc();
		User u = userStore.getUser(username);
		desc.username = u.getUsername();
		desc.password = u.getPassword();
		desc.localUserId = u.getLocalUsername();
		desc.admin = u.isAdmin();
		desc.enabled = u.isEnabled();
		return desc;
	}

	@RolesAllowed(ADMIN)
	@Override
	public Response useradd(UserDesc userdesc, UriInfo ui) {
		if (userdesc.username == null)
			throw new IllegalArgumentException("no user name supplied");
		if (userdesc.password == null)
			userdesc.password = UUID.randomUUID().toString();
		userStore.addUser(userdesc.username, userdesc.password, false);
		if (userdesc.localUserId != null)
			userStore.setUserLocalUser(userdesc.username, userdesc.localUserId);
		if (userdesc.admin != null && userdesc.admin)
			userStore.setUserAdmin(userdesc.username, true);
		if (userdesc.enabled != null && userdesc.enabled)
			userStore.setUserEnabled(userdesc.username, true);
		return Response.created(
				ui.getAbsolutePathBuilder().path("{id}")
						.build(userdesc.username)).build();
	}

	@RolesAllowed(ADMIN)
	@Override
	public UserDesc userset(String username, UserDesc userdesc) {
		if (userdesc.password != null)
			userStore.setUserPassword(username, userdesc.password);
		if (userdesc.localUserId != null)
			userStore.setUserLocalUser(username, userdesc.localUserId);
		if (userdesc.admin != null)
			userStore.setUserAdmin(username, userdesc.admin);
		if (userdesc.enabled != null)
			userStore.setUserEnabled(username, userdesc.enabled);
		userdesc = null; // Stop reuse!

		UserDesc desc = new UserDesc();
		User u = userStore.getUser(username);
		desc.username = u.getUsername();
		desc.password = u.getPassword();
		desc.localUserId = u.getLocalUsername();
		desc.admin = u.isAdmin();
		desc.enabled = u.isEnabled();
		return desc;
	}

	@RolesAllowed(ADMIN)
	@Override
	public Response userdel(String username) {
		userStore.deleteUser(username);
		return Response.noContent().build();
	}
}
