/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.commons.lifecycle;

import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Persistable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Encoder;


/**
 *
 * @author Christian Guretzki
 */
public class LifeCycleManager extends BasicManager {

	private Long   persistentRef;
	private String persistentTypeName;


	/**
	 * 
	 */
	private LifeCycleManager(Persistable persistentObject) {
		this.persistentRef  = persistentObject.getKey();
		this.persistentTypeName = getShortTypeName(persistentObject.getClass().getName());
	}

	/**
	 * Get an instance of a narrowed life-cycle manager for this olat persistentObject
	 * @param resourceable The resource
	 * @return The narrowed life-cycle manager
	 */
	public static LifeCycleManager createInstanceFor(Persistable persistentObject) {
		if (persistentObject == null) throw new AssertException("resourceable cannot be null"); 
		
		return new LifeCycleManager(persistentObject);
	}
	
	/**
	 * Create a LifeCycleEntry
	 * @return The created LifeCycleEntry
	 */
	public void markTimestampFor(String action) {
		this.markTimestampFor(action, null);
	}

	/**
	 * Create a LifeCycleEntry
	 * @return The created LifeCycleEntry
	 */
	public void markTimestampFor(String action, String userValue) {
		markTimestampFor(new Date(), action, userValue);
	}

	public void markTimestampFor(Date eventDate, String action) {
		markTimestampFor(eventDate, action, null);
	}
	
	public void markTimestampFor(Date eventDate, String action, String userValue) {
		LifeCycleEntry entry = lookupLifeCycleEntry(action, userValue);
		if (entry == null) {
			createAndSaveLifeCycleEntry(eventDate,action,userValue);
		} else {
			entry.setLcTimestamp(eventDate);
			updateLifeCycleEntry(entry);
		}
	}

	public LifeCycleEntry lookupLifeCycleEntry(String action) {
		return lookupLifeCycleEntry(action, null);
	}
	
	public LifeCycleEntry lookupLifeCycleEntry(String action, String userValue) {
		StringBuilder query = new StringBuilder("from org.olat.commons.lifecycle.LifeCycleEntry as e "); 
		query.append("where e.action=:action and e.persistentTypeName=:persistentTypeName and e.persistentRef=:persistentRef");
		if (userValue == null) {
			query.append(" and e.userValue=null");
		} else {
			query.append(" and e.userValue=:userValue");
		}
		DBQuery dbq = DBFactory.getInstance().createQuery(query.toString());
		dbq.setString("action", action);
		dbq.setString("persistentTypeName", persistentTypeName);
		dbq.setLong("persistentRef", persistentRef);
		if (userValue != null) {
			dbq.setString("userValue", userValue);
		}
		List lifeCycleEntries = dbq.list();
		
		if (lifeCycleEntries.size() == 0) {
			return null;
		} else if (lifeCycleEntries.size() > 1) {
			Tracing.logWarn("Found more than one lifeCycleObject with same parametert :" + lifeCycleEntries , this.getClass());
		}
		return (LifeCycleEntry)lifeCycleEntries.get(0);
	}
	
	
	/**
	 * Delete a LifeCycleEntry from the database
	 * @param p
	 */
	public void deleteTimestampFor(String action) {
		this.deleteTimestampFor(action, null);
	}

	/**
	 * Delete a LifeCycleEntry from the database
	 * @param p
	 */
	public void deleteTimestampFor(String action, String userValue) {
		LifeCycleEntry entry = lookupLifeCycleEntry(action, userValue);
		if (entry != null) {
		  DBFactory.getInstance().deleteObject(entry);
		}
	}

	/**
	 * Deletes all LifeCycleEntry of this resourceable
	 *
	 */
	public void deleteAllEntriesForPersistentObject() {
		// TODO:
		Tracing.logError("NOT IMPLEMENTED YET !!!!!!!!!!!", this.getClass());
	}

	/**
	 * Get a normalized type-name because type-name could be too long.
	 * @param standardTypeName
	 * @return
	 */
	public static String getShortTypeName(String standardTypeName) {
		if (standardTypeName.length() > LifeCycleEntry.PERSISTENTTYPENAME_MAXLENGTH) {
			//encode into an md5 hash with fixed length of 32 characters otherwise the sting may get too long for locks or db fields
			return Encoder.md5hash(standardTypeName);
		} else {
			return standardTypeName;
		}
	}

	///////////////////
	// Private Methods
	///////////////////

	private LifeCycleEntry createAndSaveLifeCycleEntry(Date date, String action, String userValue) {
		LifeCycleEntry entry = new LifeCycleEntry(date,persistentTypeName,persistentRef);
		entry.setAction(action);
		entry.setUserValue(userValue);
		DBFactory.getInstance().saveObject(entry);
		return entry;
	}

	private void updateLifeCycleEntry(LifeCycleEntry entry) {
		DBFactory.getInstance().updateObject(entry);
	}

}
