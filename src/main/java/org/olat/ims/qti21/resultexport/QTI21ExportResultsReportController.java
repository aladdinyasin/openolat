/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.resultexport;

import java.util.List;

import org.olat.basesecurity.GroupRoles;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.course.archiver.ScoreAccountingHelper;
import org.olat.course.nodes.QTICourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.modules.assessment.AssessmentToolOptions;
import org.springframework.beans.factory.annotation.Autowired;


public class QTI21ExportResultsReportController extends BasicController {
	
	private final Link statsButton;
	private AssessmentToolOptions asOptions;
	private QTICourseNode courseNode;
	private CourseEnvironment courseEnv;

	@Autowired
	private	BusinessGroupService groupService;
	
	public QTI21ExportResultsReportController(UserRequest ureq, WindowControl wControl,
			CourseEnvironment courseEnv, AssessmentToolOptions asOptions, QTICourseNode courseNode) {
		super(ureq, wControl);
		this.asOptions = asOptions;
		this.courseNode = courseNode;
		this.courseEnv = courseEnv;
		
		statsButton = LinkFactory.createButton("button.export", null, this);
		statsButton.setTranslator(getTranslator());
		statsButton.setIconLeftCSS("o_icon o_icon-fw o_icon_export");
		putInitialPanel(statsButton);
		getInitialComponent().setSpanAsDomReplaceable(true); // override to wrap panel as span to not break link layout 
	}
	

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// 1) calculate my assessed identities
		List<Identity> identities = asOptions.getIdentities();
		BusinessGroup group = asOptions.getGroup();
		if (group != null) {
			identities = groupService.getMembers(group, GroupRoles.participant.toString());
		} else if (identities != null) {
			identities = asOptions.getIdentities();			
		} else if (asOptions.isAdmin()){
			identities = ScoreAccountingHelper.loadUsers(courseEnv);
		}
		if (identities != null && identities.size() > 0) {
			// 2) create export resource
			MediaResource resource = new QTI21ResultsExportMediaResource(courseEnv, identities, courseNode, getLocale());
			// 3) download
			ureq.getDispatchResult().setResultingMediaResource(resource);

		} else {
			showWarning("error.no.assessed.users");
		}
		
	}

	@Override
	protected void doDispose() {
		//
	}
}
