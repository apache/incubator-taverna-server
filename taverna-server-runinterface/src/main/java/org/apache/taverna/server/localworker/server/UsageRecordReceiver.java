/*
 */
package org.apache.taverna.server.localworker.server;
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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface exported by (part of) the webapp to allow processes it creates to
 * push in usage records.
 * 
 * @author Donal Fellows
 */
public interface UsageRecordReceiver extends Remote {
	/**
	 * Called to push in a usage record. Note that it is assumed that the usage
	 * record already contains all the information required to locate and
	 * process the job; there is no separate handle.
	 * 
	 * @param usageRecord
	 *            The serialised XML of the usage record.
	 * @throws RemoteException
	 *             if anything goes wrong.
	 */
	void acceptUsageRecord(String usageRecord) throws RemoteException;
}
