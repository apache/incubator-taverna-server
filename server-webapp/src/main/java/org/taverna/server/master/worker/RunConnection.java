/*
 * Copyright (C) 2010-2013 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.worker;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.taverna.server.master.worker.RunConnection.COUNT_QUERY;
import static org.taverna.server.master.worker.RunConnection.NAMES_QUERY;
import static org.taverna.server.master.worker.RunConnection.SCHEMA;
import static org.taverna.server.master.worker.RunConnection.TABLE;
import static org.taverna.server.master.worker.RunConnection.TIMEOUT_QUERY;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;

import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.master.common.Credential;
import org.taverna.server.master.common.Trust;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.interfaces.SecurityContextFactory;
import org.taverna.server.master.utils.UsernamePrincipal;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * The representation of the connections to the runs that actually participates
 * in the persistence system.
 * 
 * @author Donal Fellows
 */
@PersistenceCapable(table = TABLE, schema = SCHEMA)
@Queries({
		@Query(name = "count", language = "SQL", value = COUNT_QUERY, unique = "true", resultClass = Integer.class),
		@Query(name = "names", language = "SQL", value = NAMES_QUERY, unique = "false", resultClass = String.class),
		@Query(name = "timedout", language = "SQL", value = TIMEOUT_QUERY, unique = "false", resultClass = String.class) })
@SuppressWarnings("IS2_INCONSISTENT_SYNC")
public class RunConnection {
	static final String SCHEMA = "TAVERNA";
	static final String TABLE = "RUN_CONNECTION";
	private static final String FULL_NAME = SCHEMA + "." + TABLE;
	static final String COUNT_QUERY = "SELECT count(*) FROM " + FULL_NAME;
	static final String NAMES_QUERY = "SELECT ID FROM " + FULL_NAME;
	static final String TIMEOUT_QUERY = "SELECT ID FROM " + FULL_NAME
			+ "   WHERE expiry < CURRENT_TIMESTAMP";

	@PrimaryKey
	@Column(length = 40)
	private String id;

	@Persistent(defaultFetchGroup = "true")
	private Date creationInstant;

	@Persistent(defaultFetchGroup = "true", serialized = "true")
	@Column(jdbcType = "BLOB", sqlType = "BLOB")
	private Workflow workflow;

	@Persistent(defaultFetchGroup = "true")
	private Date expiry;

	@Persistent(defaultFetchGroup = "true")
	@Join(table = TABLE + "_READERS", column = "ID")
	private String[] readers;

	@Persistent(defaultFetchGroup = "true")
	@Join(table = TABLE + "_WRITERS", column = "ID")
	private String[] writers;

	@Persistent(defaultFetchGroup = "true")
	@Join(table = TABLE + "_DESTROYERS", column = "ID")
	private String[] destroyers;

	@Persistent(defaultFetchGroup = "true", serialized = "true")
	@Column(jdbcType = "BLOB", sqlType = "BLOB")
	private MarshalledObject<RemoteSingleRun> run;

	@Persistent(defaultFetchGroup = "true")
	private int doneTransitionToFinished;

	@Persistent(defaultFetchGroup = "true")
	@Column(length = 128)
	String owner;

	@Persistent(defaultFetchGroup = "true")
	@Column(length = 36)
	private String securityToken;

	@Persistent(defaultFetchGroup = "true", serialized = "true")
	@Column(jdbcType = "BLOB", sqlType = "BLOB")
	private SecurityContextFactory securityContextFactory;
	@Persistent(defaultFetchGroup = "true", serialized = "true")
	@Column(jdbcType = "BLOB", sqlType = "BLOB")
	private Credential[] credentials;
	@Persistent(defaultFetchGroup = "true", serialized = "true")
	@Column(jdbcType = "BLOB", sqlType = "BLOB")
	private Trust[] trust;

	private static final String[] STRING_ARY = new String[0];

	public String getId() {
		return id;
	}

	public boolean isFinished() {
		return doneTransitionToFinished != 0;
	}

	public void setFinished(boolean finished) {
		doneTransitionToFinished = (finished ? 1 : 0);
	}

	/**
	 * Manufacture a persistent representation of the given workflow run. Must
	 * be called within the context of a transaction.
	 * 
	 * @param rrd
	 *            The remote delegate of the workflow run.
	 * @return The persistent object.
	 * @throws IOException
	 *             If serialisation fails.
	 */
	@NonNull
	public static RunConnection toDBform(@NonNull RemoteRunDelegate rrd)
			throws IOException {
		RunConnection rc = new RunConnection();
		rc.id = rrd.id;
		rc.makeChanges(rrd);
		return rc;
	}

	private static List<String> list(String[] ary) {
		if (ary == null)
			return emptyList();
		return asList(ary);
	}

	/**
	 * Get the remote run delegate for a particular persistent connection. Must
	 * be called within the context of a transaction.
	 * 
	 * @param db
	 *            The database facade.
	 * @return The delegate object.
	 * @throws Exception
	 *             If anything goes wrong.
	 */
	@NonNull
	public RemoteRunDelegate fromDBform(@NonNull RunDBSupport db)
			throws Exception {
		RemoteRunDelegate rrd = new RemoteRunDelegate();
		rrd.id = getId();
		rrd.creationInstant = creationInstant;
		rrd.workflow = workflow;
		rrd.expiry = expiry;
		rrd.readers = new HashSet<String>(list(readers));
		rrd.writers = new HashSet<String>(list(writers));
		rrd.destroyers = new HashSet<String>(list(destroyers));
		rrd.run = run.get();
		rrd.doneTransitionToFinished = isFinished();
		rrd.secContext = securityContextFactory.create(rrd,
				new UsernamePrincipal(owner));
		((SecurityContextDelegate)rrd.secContext).setCredentialsAndTrust(credentials,trust);
		rrd.db = db;
		rrd.factory = db.getFactory();
		return rrd;
	}

	/**
	 * Flush changes from a remote run delegate to the database. Must be called
	 * within the context of a transaction.
	 * 
	 * @param rrd
	 *            The remote run delegate object that has potential changes.
	 * @throws IOException
	 *             If anything goes wrong in serialization.
	 */
	public void makeChanges(@NonNull RemoteRunDelegate rrd) throws IOException {
		// Properties that are set exactly once
		if (creationInstant == null) {
			creationInstant = rrd.getCreationTimestamp();
			workflow = rrd.getWorkflow();
			run = new MarshalledObject<RemoteSingleRun>(rrd.run);
			securityContextFactory = rrd.getSecurityContext().getFactory();
			owner = rrd.getSecurityContext().getOwner().getName();
			securityToken = randomUUID().toString();
		}
		// Properties that are set multiple times
		expiry = rrd.getExpiry();
		readers = rrd.getReaders().toArray(STRING_ARY);
		writers = rrd.getWriters().toArray(STRING_ARY);
		destroyers = rrd.getDestroyers().toArray(STRING_ARY);
		credentials = rrd.getSecurityContext().getCredentials();
		trust = rrd.getSecurityContext().getTrusted();
		setFinished(rrd.doneTransitionToFinished);
	}

	public String getSecurityToken() {
		return securityToken;
	}
}
