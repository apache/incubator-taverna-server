/*
 * Copyright (C) 2010-2011 The University of Manchester
 * 
 * See the file "LICENSE.txt" for license terms.
 */
package org.taverna.server.master.localworker;

import static java.util.Arrays.asList;
import static org.taverna.server.master.localworker.RunConnection.ALL_QUERY;
import static org.taverna.server.master.localworker.RunConnection.COUNT_QUERY;
import static org.taverna.server.master.localworker.RunConnection.NAMES_QUERY;
import static org.taverna.server.master.localworker.RunConnection.PICK_QUERY;
import static org.taverna.server.master.localworker.RunConnection.SCHEMA;
import static org.taverna.server.master.localworker.RunConnection.TABLE;
import static org.taverna.server.master.localworker.RunConnection.TIMEOUT_QUERY;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.Date;
import java.util.HashSet;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Serialized;

import org.apache.cxf.common.security.SimplePrincipal;
import org.taverna.server.localworker.remote.RemoteSingleRun;
import org.taverna.server.master.common.Workflow;
import org.taverna.server.master.interfaces.SecurityContextFactory;

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
		@Query(name = "all", value = ALL_QUERY),
		@Query(name = "count", language = "SQL", value = COUNT_QUERY, unique = "true", resultClass = Integer.class),
		@Query(name = "names", language = "SQL", value = NAMES_QUERY, unique = "false", resultClass = String.class),
		@Query(name = "pick", value = PICK_QUERY, unique = "true"),
		@Query(name = "timedout", value = TIMEOUT_QUERY) })
@SuppressWarnings("IS2_INCONSISTENT_SYNC")
public class RunConnection {
	static final String SCHEMA = "TAVERNA";
	static final String TABLE = "RUN_CONNECTION";
	static final String ALL_QUERY = "SELECT FROM org.taverna.server.master.localworker.RunConnection";
	static final String COUNT_QUERY = "SELECT count(*) FROM " + SCHEMA + "."
			+ TABLE;
	static final String NAMES_QUERY = "SELECT ID FROM " + SCHEMA + "." + TABLE;
	static final String PICK_QUERY = ALL_QUERY + " WHERE this.ID = name"
			+ " PARAMETERS String name import java.lang.String";
	static final String TIMEOUT_QUERY = ALL_QUERY
			+ " WHERE this.EXPIRY < currentTime"
			+ " PARAMETERS Date currentTime import java.util.Date";

	@PrimaryKey
	@Column(length = 40)
	private String id;

	@Persistent(defaultFetchGroup = "true")
	private Date creationInstant;

	@Persistent(defaultFetchGroup = "true")
	@Serialized
	// @Column(jdbcType = "BLOB")
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

	@Persistent(defaultFetchGroup = "true")
	@Serialized
	private MarshalledObject<RemoteSingleRun> run;

	@Persistent(defaultFetchGroup = "true")
	private int doneTransitionToFinished;

	@Persistent(defaultFetchGroup = "true")
	@Column(length = 128)
	String owner;

	@Persistent(defaultFetchGroup = "true")
	@Serialized
	private SecurityContextFactory securityContextFactory;

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

	@NonNull
	public static RunConnection toDBform(@NonNull RemoteRunDelegate rrd)
			throws IOException {
		RunConnection rc = new RunConnection();
		rc.id = rrd.id;
		rc.creationInstant = rrd.getCreationTimestamp();
		rc.workflow = rrd.getWorkflow();
		rc.expiry = rrd.getExpiry();
		rc.readers = rrd.getReaders().toArray(STRING_ARY);
		rc.writers = rrd.getWriters().toArray(STRING_ARY);
		rc.destroyers = rrd.getDestroyers().toArray(STRING_ARY);
		rc.run = new MarshalledObject<RemoteSingleRun>(rrd.run);
		rc.setFinished(rrd.doneTransitionToFinished);
		rc.owner = rrd.getSecurityContext().getOwner().getName();
		rc.securityContextFactory = rrd.getSecurityContext().getFactory();
		return rc;
	}

	@NonNull
	public RemoteRunDelegate fromDBform() throws Exception {
		RemoteRunDelegate rrd = new RemoteRunDelegate();
		rrd.id = getId();
		rrd.creationInstant = creationInstant;
		rrd.workflow = workflow;
		rrd.expiry = expiry;
		rrd.readers = new HashSet<String>(asList(readers));
		rrd.writers = new HashSet<String>(asList(writers));
		rrd.destroyers = new HashSet<String>(asList(destroyers));
		rrd.run = run.get();
		rrd.doneTransitionToFinished = isFinished();
		rrd.secContext = securityContextFactory.create(rrd,
				new SimplePrincipal(owner));
		return rrd;
	}
}