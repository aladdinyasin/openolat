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

package org.olat.course.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.olat.basesecurity.IdentityImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.nodes.CourseNode;
import org.olat.group.BusinessGroup;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

/**
 * Initial Date: May 5, 2004
 * 
 * @author gnaegi<br>
 *         Comment: The course property manager handles all course specific read /
 *         write operations for course properties. Usually other managers use
 *         the course property manager (like assessment manager or audit
 *         manager), only view controllers use the course property manager
 *         directly.
 */
public class PersistingCoursePropertyManager extends BasicManager implements CoursePropertyManager {

	private NarrowedPropertyManager pm;
	private Map<String,String> anonymizerMap;
	private OLATResourceable ores;
	private static Random random = new Random(System.currentTimeMillis());

	private PersistingCoursePropertyManager(OLATResourceable course) {
		this.ores = course;
		pm = NarrowedPropertyManager.getInstance(course);
		// Initialize identity anonymizer map
		anonymizerMap = new HashMap<String,String>();
	}

	/**
	 * Get an instance of the course property manager for this course that
	 * persists properties to the database.
	 * 
	 * @param olatCourse The course
	 * @return The course property manager
	 */
	public static PersistingCoursePropertyManager getInstance(OLATResourceable course) {
		return new PersistingCoursePropertyManager(course);
	}
	
	public OLATResource getCourseResource() {
		return OLATResourceManager.getInstance().findResourceable(ores);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#createCourseNodePropertyInstance(org.olat.course.nodes.CourseNode,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      java.lang.String, java.lang.Float, java.lang.Long, java.lang.String,
	 *      java.lang.String)
	 */
	public Property createCourseNodePropertyInstance(CourseNode node, Identity identity, BusinessGroup group, String name, Float floatValue,
			Long longValue, String stringValue, String textValue) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.createPropertyInstance(identity, group, myCategory, name, floatValue, longValue, stringValue, textValue);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteProperty(org.olat.properties.Property)
	 */
	public void deleteProperty(Property p) {
		pm.deleteProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#saveProperty(org.olat.properties.Property)
	 */
	public void saveProperty(Property p) {
		pm.saveProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#updateProperty(org.olat.properties.Property)
	 */
	public void updateProperty(Property p) {
		pm.updateProperty(p);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#listCourseNodeProperties(org.olat.course.nodes.CourseNode,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      java.lang.String)
	 */
	@Override
	public List<Property> listCourseNodeProperties(CourseNode node, Identity identity, BusinessGroup grp, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.listProperties(identity, grp, myCategory, name);
	}

	@Override
	public int countCourseNodeProperties(CourseNode node, Identity identity, BusinessGroup grp, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.countProperties(identity, grp, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperties(org.olat.course.nodes.CourseNode,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      java.lang.String)
	 */
	@Override
	public List<Property> findCourseNodeProperties(CourseNode node, Identity identity, BusinessGroup grp, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperties(identity, grp, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#findCourseNodeProperty(org.olat.course.nodes.CourseNode,
	 *      org.olat.core.id.Identity, org.olat.group.BusinessGroup,
	 *      java.lang.String)
	 */
	@Override
	public Property findCourseNodeProperty(CourseNode node, Identity identity, BusinessGroup grp, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperty(identity, grp, myCategory, name);
	}

	@Override
	public Property findCourseNodeProperty(CourseNode node, BusinessGroup grp, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperty(grp, myCategory, name);
	}

	@Override
	public List<Property> findCourseNodeProperties(CourseNode node, List<Identity> identities, String name) {
		String myCategory = buildCourseNodePropertyCategory(node);
		return pm.findProperties(identities, myCategory, name);
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteNodeProperties(org.olat.course.nodes.CourseNode,
	 *      java.lang.String)
	 */
	public void deleteNodeProperties(CourseNode courseNode, String name) {
		if (courseNode == null) { throw new AssertException("courseNode can not be null when deleting course node properties"); }
		pm.deleteProperties(null, null, buildCourseNodePropertyCategory(courseNode), name);
	}

	private String buildCourseNodePropertyCategory(CourseNode node) {
		String type = (node.getType().length() > 4 ? node.getType().substring(0, 4) : node.getType());
		return ("NID:" + type + "::" + node.getIdent());
	}

	/**
	 * @see org.olat.ims.qti.export.helper.IdentityAnonymizerCallback#getAnonymizedUserName(org.olat.core.id.Identity)
	 */
	public String getAnonymizedUserName(Identity identity) {
		synchronized (anonymizerMap) {
			String anonymizedName = anonymizerMap.get(identity.getName());
			if (anonymizedName == null) {
				// try to fetch from course properties
				Property anonymizedProperty = pm.findProperty(identity, null, "Anonymizing", "AnonymizedUserName");
				if (anonymizedProperty == null) {
					// not found - create a new anonymized name
					anonymizedName = "RANDOM-" + random.nextInt(100000000);
					// add as course properties
					anonymizedProperty = pm.createPropertyInstance(identity, null, "Anonymizing", "AnonymizedUserName", null, null, anonymizedName,
							null);
					pm.saveProperty(anonymizedProperty);
				} else {
					// property found - use property value from there
					anonymizedName = anonymizedProperty.getStringValue();
				}
				anonymizerMap.put(identity.getName(), anonymizedName);
			}
			return anonymizedName;
		}
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#deleteAllCourseProperties()
	 */
	public void deleteAllCourseProperties() {
		pm.deleteAllProperties();
	}

	/**
	 * @see org.olat.course.properties.CoursePropertyManager#getAllIdentitiesWithCourseAssessmentData()
	 */
	public List<Identity> getAllIdentitiesWithCourseAssessmentData(Collection<Identity> excludeIdentities) {
		StringBuilder query = new StringBuilder();
		query.append("select distinct i from ")
			.append(IdentityImpl.class.getName()).append(" as i,")
			.append(Property.class.getName()).append(" as p")
			.append(" where i = p.identity and p.resourceTypeName = :resname")
			.append(" and p.resourceTypeId = :resid")
			.append(" and p.identity is not null")
			.append(" and p.name in ('").append(AssessmentManager.SCORE).append("','").append(AssessmentManager.PASSED).append("')");
		
		if(excludeIdentities != null && !excludeIdentities.isEmpty()) {
			query.append(" and p.identity.key not in (:excludeIdentities) ");
		}

		DB db = DBFactory.getInstance();
		DBQuery dbq = db.createQuery(query.toString());
		dbq.setLong("resid", ores.getResourceableId());
		dbq.setString("resname", ores.getResourceableTypeName());
		if(excludeIdentities != null && !excludeIdentities.isEmpty()) {
			List<Long> excludeKeys = new ArrayList<Long>();
			for(Identity identity:excludeIdentities) {
				excludeKeys.add(identity.getKey());
			}
			dbq.setParameterList("excludeIdentities", excludeKeys);
		}

		List<Identity> res = dbq.list();
		return res;
	}

}