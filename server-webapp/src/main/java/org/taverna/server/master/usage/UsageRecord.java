/*
 * Copyright (C) 2011 The University of Manchester
 * 
 * See the file "LICENSE" for license terms.
 */
package org.taverna.server.master.usage;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.xml.bind.JAXBException;

import org.ogf.usage.JobUsageRecord;

/**
 * A usage record as recorded in the database.
 * 
 * @author Donal Fellows
 */
@PersistenceCapable(table = "USAGE_RECORD_LOG", schema = "UR", cacheable = "true")
@Queries({ @Query(name = "allByDate", value = "SELECT USAGE_RECORD FROM UR.USAGE_RECORD_LOG ORDER BY CREATE_DATE", resultClass = String.class, unmodifiable = "true", unique = "false", language = "SQL") })
public class UsageRecord {
	/**
	 * Create an empty usage record database entry.
	 */
	public UsageRecord() {
	}

	/**
	 * Create a usage record database entry that is populated from the given UR.
	 * 
	 * @param usageRecord
	 *            The originating usage record.
	 * @throws JAXBException
	 *             If deserialization of the record fails.
	 */
	public UsageRecord(String usageRecord) throws JAXBException {
		JobUsageRecord jur = JobUsageRecord.unmarshal(usageRecord);
		setUsageRecord(usageRecord);
		setCreateDate(jur.getRecordIdentity().getCreateTime()
				.toGregorianCalendar().getTime());
		setId(jur.getRecordIdentity().getRecordId());
		setUserid(jur.getUserIdentity().get(0).getLocalUserId());
	}

	/**
	 * Create a usage record database entry that is populated from the given UR.
	 * 
	 * @param usageRecord
	 *            The originating usage record.
	 * @throws JAXBException
	 *             If serialization of the record fails.
	 */
	public UsageRecord(JobUsageRecord usageRecord) throws JAXBException {
		setUsageRecord(usageRecord.marshal());
		setCreateDate(usageRecord.getRecordIdentity().getCreateTime()
				.toGregorianCalendar().getTime());
		setId(usageRecord.getRecordIdentity().getRecordId());
		setUserid(usageRecord.getUserIdentity().get(0).getLocalUserId());
	}

	@PrimaryKey
	@Column(name = "ID", length = 40)
	private String id;

	@Persistent
	@Index(name = "USERID_IDX")
	@Column(name = "USERID", length = 24)
	private String userid;

	@Persistent
	@Index(name = "CREATE_IDX")
	@Column(name = "CREATE_DATE")
	private Date createDate;

	@Persistent
	@Column(name = "USAGE_RECORD", length = 32000)
	private String usageRecord;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getUsageRecord() {
		return usageRecord;
	}

	public void setUsageRecord(String usageRecord) {
		this.usageRecord = usageRecord;
	}
}