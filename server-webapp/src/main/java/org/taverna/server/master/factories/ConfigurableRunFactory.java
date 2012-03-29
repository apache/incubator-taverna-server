/*
 * Copyright (C) 2012 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.factories;

/**
 * Interface to run factories for the purpose of configuration.
 * 
 * @author Donal Fellows
 */
public interface ConfigurableRunFactory extends RunFactory {
	/** Where is the registry? Getter */
	String getRegistryHost();

	/** Where is the registry? Setter */
	void setRegistryHost(String host);
	
	/** Where is the registry? Getter */
	int getRegistryPort();
	
	/** Where is the registry? Setter */
	void setRegistryPort(int port);

	/** How much can be done at once? Getter */
	int getMaxRuns();

	/** How much can be done at once? Setter */
	void setMaxRuns(int maxRuns);

	/** How long will things live? Getter */
	int getDefaultLifetime();

	/** How long will things live? Setter */
	void setDefaultLifetime(int defaultLifetime);

	/** How often do we probe for info? Getter */
	int getSleepTime();
	
	/** How often do we probe for info? Setter */
	void setSleepTime(int sleepTime);

	/** How long do we allow for actions? Getter */
	int getWaitSeconds();
	
	/** How long do we allow for actions? Setter */
	void setWaitSeconds(int seconds);

	/** How do we start the workflow engine? Getter */
	String getExecuteWorkflowScript();

	/** How do we start the workflow engine? Setter */
	void setExecuteWorkflowScript(String executeWorkflowScript);

	/** How do we start the file system access process? Getter */
	String getServerWorkerJar();

	/** How do we start the file system access process? Setter */
	void setServerWorkerJar(String serverWorkerJar);

	/** How do we start the file system access process? Extra arguments to pass. Getter */
	String[] getExtraArguments();

	/** How do we start the file system access process? Extra arguments to pass. Setter */
	void setExtraArguments(String[] firstArguments);

	/** Where is Java? Getter */
	String getJavaBinary();

	/** Where is Java? Setter */
	void setJavaBinary(String javaBinary);

	/** Where do we get passwords from? Getter */
	String getPasswordFile();

	/** Where do we get passwords from? Setter */
	void setPasswordFile(String newValue);

	/** How do we switch users? Getter */
	String getServerForkerJar();

	/** How do we switch users? Setter */
	void setServerForkerJar(String newValue);

	/** How many runs have there been? */
	int getTotalRuns();
	
	/** How long did the last subprocess startup take? */
	int getLastStartupCheckCount();
	
	/** What are the current runs? */
	String[] getCurrentRunNames();

	/** What is the RMI ID of the factory process? */
	String getFactoryProcessName();

	/** What was the last observed exit code? */
	Integer getLastExitCode();

	/** What factory process to use for a particular user? */
	String[] getFactoryProcessMapping();

}
