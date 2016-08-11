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
package org.olat.course.nodes.gta.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.ui.tool.AssessmentIdentityCourseNodeController;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.GTAManager;
import org.olat.course.nodes.gta.Task;
import org.olat.course.nodes.gta.TaskProcess;
import org.olat.course.nodes.ms.MSCourseNodeRunController;
import org.olat.course.run.scoring.AssessmentEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.assessment.model.AssessmentEntryStatus;
import org.olat.modules.assessment.ui.event.AssessmentFormEvent;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 18.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GTACoachedParticipantGradingController extends BasicController {
	
	private final Link assessmentFormButton;
	private final VelocityContainer mainVC;
	
	private CloseableModalController cmc;
	private MSCourseNodeRunController msCtrl;
	private AssessmentIdentityCourseNodeController assessmentForm;
	
	private Task assignedTask;
	private final GTACourseNode gtaNode;
	private final Identity assessedIdentity;
	private final OLATResourceable courseOres;

	@Autowired
	private GTAManager gtaManager;
	
	public GTACoachedParticipantGradingController(UserRequest ureq, WindowControl wControl,
			OLATResourceable courseOres, GTACourseNode gtaNode, Task assignedTask, Identity assessedIdentity) {
		super(ureq, wControl);
		this.gtaNode = gtaNode;
		this.assignedTask = assignedTask;
		this.courseOres = OresHelper.clone(courseOres);
		this.assessedIdentity = assessedIdentity;
		
		mainVC = createVelocityContainer("coach_grading");
		
		assessmentFormButton = LinkFactory.createCustomLink("coach.assessment", "assessment", "coach.assessment", Link.BUTTON, mainVC, this);
		assessmentFormButton.setCustomEnabledLinkCSS("btn btn-primary");
		assessmentFormButton.setIconLeftCSS("o_icon o_icon o_icon_submit");
		assessmentFormButton.setElementCssClass("o_sel_course_gta_assessment_button");

		putInitialPanel(mainVC);
		setAssessmentDatas(ureq);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(assessmentForm == source) {
			if(event instanceof AssessmentFormEvent) {
				UserCourseEnvironment assessedUserCourseEnv = assessmentForm.getAssessedUserCourseEnvironment();
				doGraded(ureq, assessedUserCourseEnv);
			}
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(assessmentForm);
		removeAsListenerAndDispose(cmc);
		assessmentForm = null;
		cmc = null;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(assessmentFormButton == source) {
			doOpenAssessmentForm(ureq);
		}
	}
	
	private void setAssessmentDatas(UserRequest ureq) {
		removeAsListenerAndDispose(msCtrl);
		
		ICourse course = CourseFactory.loadCourse(courseOres);
		UserCourseEnvironment uce = AssessmentHelper.createAndInitUserCourseEnvironment(assessedIdentity, course);
		msCtrl = new MSCourseNodeRunController(ureq, getWindowControl(), uce, gtaNode, false, false);
		listenTo(msCtrl);
		mainVC.put("msrun", msCtrl.getInitialComponent());
	}
	
	private void doGraded(UserRequest ureq, UserCourseEnvironment assessedUserCourseEnv) {
		removeAsListenerAndDispose(msCtrl);
		msCtrl = new MSCourseNodeRunController(ureq, getWindowControl(), assessedUserCourseEnv, gtaNode, false, false);
		listenTo(msCtrl);
		mainVC.put("msrun", msCtrl.getInitialComponent());
		
		AssessmentEvaluation scoreEval = gtaNode.getUserScoreEvaluation(assessedUserCourseEnv);
		if(scoreEval.getAssessmentStatus() == AssessmentEntryStatus.done) {
			assignedTask = gtaManager.updateTask(assignedTask, TaskProcess.graded, gtaNode);
			fireEvent(ureq, Event.CHANGED_EVENT);
		}
	}

	private void doOpenAssessmentForm(UserRequest ureq) {
		if(assessmentForm != null) return;//already open
		
		RepositoryEntry courseEntry = CourseFactory.loadCourse(courseOres).getCourseEnvironment().getCourseGroupManager().getCourseEntry();

		assessmentForm = new AssessmentIdentityCourseNodeController(ureq, getWindowControl(), null, courseEntry, gtaNode, assessedIdentity, false);
		listenTo(assessmentForm);
		
		String title = translate("grading");
		cmc = new CloseableModalController(getWindowControl(), "close", assessmentForm.getInitialComponent(), true, title, true);
		listenTo(cmc);
		cmc.activate();
	}
}
