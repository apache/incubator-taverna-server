/*
 */
package org.apache.taverna.server.master.usage;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.xml.bind.JAXBException;

import org.apache.taverna.server.usagerecord.JobUsageRecord;

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
	@Column(name = "USAGE_RECORD", jdbcType = "CLOB")
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