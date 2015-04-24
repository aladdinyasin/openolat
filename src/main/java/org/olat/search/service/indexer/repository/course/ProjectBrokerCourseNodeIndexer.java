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

package org.olat.search.service.indexer.repository.course;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.core.CoreSpringFactory;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManager;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.ProjectBrokerProjectDocument;
import org.olat.search.service.indexer.AbstractHierarchicalIndexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Indexer for project-broker course-node.
 * @author Christian Guretzki
 */
public class ProjectBrokerCourseNodeIndexer extends AbstractHierarchicalIndexer implements CourseNodeIndexer {
	private static final OLog log = Tracing.createLoggerFor(ProjectBrokerCourseNodeIndexer.class); 

	public static final String TYPE = "type.course.node.projectbroker";

	private final static String SUPPORTED_TYPE_NAME = "org.olat.course.nodes.ProjectBrokerCourseNode";

	@Override
	public void doIndex(SearchResourceContext repositoryResourceContext, ICourse course, CourseNode courseNode, OlatFullIndexer indexWriter) throws IOException,InterruptedException  {
		SearchResourceContext courseNodeResourceContext = new SearchResourceContext(repositoryResourceContext);
		courseNodeResourceContext.setBusinessControlFor(courseNode);
		courseNodeResourceContext.setDocumentType(TYPE);
		courseNodeResourceContext.setTitle(courseNode.getShortTitle());
		courseNodeResourceContext.setDescription(courseNode.getLongTitle());
       
		// go further, index my projects
		CoursePropertyManager cpm = course.getCourseEnvironment().getCoursePropertyManager();
		ProjectBrokerManager projectBrokerManager = CoreSpringFactory.getImpl(ProjectBrokerManager.class);
		Long projectBrokerId = projectBrokerManager.getProjectBrokerId(cpm, courseNode);
		if (projectBrokerId != null) {
			List<Project> projects = projectBrokerManager.getProjectListBy(projectBrokerId);
			for (Iterator<Project> iterator = projects.iterator(); iterator.hasNext();) {
				Project project = iterator.next();
				Document document = ProjectBrokerProjectDocument.createDocument(courseNodeResourceContext, project);
				indexWriter.addDocument(document);
			}
		} else {
			log.debug("projectBrokerId is null, courseNode=" + courseNode + " , course=" + course);
		}
	}

	@Override
	public String getSupportedTypeName() {
		return SUPPORTED_TYPE_NAME;
	}
}
