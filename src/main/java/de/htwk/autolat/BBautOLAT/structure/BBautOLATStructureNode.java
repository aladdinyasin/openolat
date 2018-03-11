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
* <p>
*/ 

package de.htwk.autolat.BBautOLAT.structure;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.core.commons.controllers.linkchooser.CustomLinkTreeModel;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.commons.modules.singlepage.SinglePageController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.clone.CloneController;
import org.olat.core.gui.control.generic.clone.CloneLayoutControllerCreatorCallback;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.assessment.ui.tool.AssessmentCourseNodeController;
import org.olat.course.assessment.ui.tool.IdentityListCourseNodeController;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionExpression;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.StatusDescriptionHelper;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.AssessmentEvaluation;
import org.olat.course.run.scoring.ScoreCalculator;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.group.BusinessGroup;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.assessment.Role;
import org.olat.modules.assessment.model.AssessmentRunStatus;
import org.olat.modules.assessment.ui.AssessmentToolContainer;
import org.olat.modules.assessment.ui.AssessmentToolSecurityCallback;
import org.olat.repository.RepositoryEntry;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<br>
 * The autOlat Structure Node adds different Tables for different Roles to the original ST Node.
 * 
 * Students will see a anonymous Highscorelist. <br>
 * Advisors will see a Table where they may edit the Results for each participant (e.g. passed/not passed or the points)
 * 
 * <P>
 * Initial Date: Feb 9, 2004<br>
 * @author Mike Stock
 * @author BPS (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class BBautOLATStructureNode extends AbstractAccessableCourseNode implements AssessableCourseNode {

	private static final String TYPE = "autolatst";

	private ScoreCalculator scoreCalculator;

	transient private Condition scoreExpression;

	transient private Condition passedExpression; 

	/**
	 * Constructor for a course building block of the type structure
	 */
	public BBautOLATStructureNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		// only the precondition "access" can be configured till now
		BBautOLATStructureNodeEditController childTabCntrllr;
		childTabCntrllr = new BBautOLATStructureNodeEditController(ureq, wControl, this, course.getCourseFolderContainer(), 
				course.getCourseEnvironment().getCourseGroupManager(), course.getEditorTreeModel(), euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			final UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		// evaluate the score accounting for this node. this uses the score accountings local
		// cache hash map to reduce unnecessary calculations
		ScoreEvaluation se = userCourseEnv.getScoreAccounting().evalCourseNode(this);
		BBautOLATStructureNodeRunController cont = new BBautOLATStructureNodeRunController(ureq, wControl, userCourseEnv, this, se, ne);

		// access the current calculated score, if there is one, so that it can be
		// displayed in the ST-Runcontroller
		return new NodeRunConstructionResult(cont);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPreviewController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPreviewController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		return createNodeRunConstructionResult(ureq, wControl, userCourseEnv, ne, null).getRunController();
	}

	/**
	 * the structure node does not have a score itself, but calculates the
	 * score/passed info by evaluating the configured expression in the the
	 * (condition)interpreter.
	 * 
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserScoreEvaluation(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public AssessmentEvaluation getUserScoreEvaluation(UserCourseEnvironment userCourseEnv) {
		Float score = null;
		Boolean passed = null;

		if (scoreCalculator == null) { 
			// this is a not-computable course node at the moment (no scoring/passing rules defined)
			return null; 
		}
		String scoreExpressionStr = scoreCalculator.getScoreExpression();
		String passedExpressionStr = scoreCalculator.getPassedExpression();

		ConditionInterpreter ci = userCourseEnv.getConditionInterpreter();
		if (scoreExpressionStr != null) {
			score = new Float(ci.evaluateCalculation(scoreExpressionStr));
		}
		if (passedExpressionStr != null) {
			passed = new Boolean(ci.evaluateCondition(passedExpressionStr));
		}
		return new AssessmentEvaluation(score, passed);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		return StatusDescription.NOERROR;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		String translatorStr = Util.getPackageName(BBautOLATStructureNodeEditController.class);
		List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * @return Returns the scoreCalculator.
	 */
	public ScoreCalculator getScoreCalculator() {
		if (scoreCalculator == null) {
			scoreCalculator = new ScoreCalculator();
		}
		passedExpression = new Condition();
		passedExpression.setConditionId("passed");
		if (scoreCalculator.getPassedExpression() != null) {
			passedExpression.setConditionExpression(scoreCalculator.getPassedExpression());
			passedExpression.setExpertMode(true);
		}
		scoreExpression = new Condition();
		scoreExpression.setConditionId("score");
		if (scoreCalculator.getScoreExpression() != null) {
			scoreExpression.setConditionExpression(scoreCalculator.getScoreExpression());
			scoreExpression.setExpertMode(true);
		}
		return scoreCalculator;
	}

	/**
	 * @param scoreCalculator The scoreCalculator to set.
	 */
	public void setScoreCalculator(ScoreCalculator scoreCalculatorP) {
		scoreCalculator = scoreCalculatorP;
		if (scoreCalculatorP == null) {
			scoreCalculator = getScoreCalculator();
		}
		String passed, score;
		passed = scoreCalculator.getPassedExpression();
		score = scoreCalculator.getScoreExpression();
		scoreExpression.setExpertMode(true);
		scoreExpression.setConditionExpression(score);
		scoreExpression.setConditionId("score");
		passedExpression.setExpertMode(true);
		passedExpression.setConditionExpression(passed);
		passedExpression.setConditionId("passed");
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getCutValueConfiguration()
	 */
	@Override
	public Float getCutValueConfiguration() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMaxScoreConfiguration()
	 */
	@Override
	public Float getMaxScoreConfiguration() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getMinScoreConfiguration()
	 */
	@Override
	public Float getMinScoreConfiguration() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserCoachComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserCoachComment(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "No coach comments available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserLog(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserLog(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "No user logs available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserUserComment(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getUserUserComment(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "No comments available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasCommentConfigured()
	 */
	@Override
	public boolean hasCommentConfigured() {
		// never has comments
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasPassedConfigured()
	 */
	@Override
	public boolean hasPassedConfigured() {
		if (scoreCalculator != null && StringHelper.containsNonWhitespace(scoreCalculator.getPassedExpression())) return true;
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasScoreConfigured()
	 */
	@Override
	public boolean hasScoreConfigured() {
		if (scoreCalculator != null && StringHelper.containsNonWhitespace(scoreCalculator.getScoreExpression())) return true;
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasStatusConfigured()
	 */
	@Override
	public boolean hasStatusConfigured() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#isEditableConfigured()
	 */
	@Override
	public boolean isEditableConfigured() {
		// ST nodes never editable, data generated on the fly
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserCoachComment(java.lang.String,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void updateUserCoachComment(String coachComment, UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Coach comment variable can't be updated in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserScoreEvaluation(org.olat.course.run.scoring.ScoreEvaluation,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserScoreEvaluation(ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnvironment,
			Identity coachingIdentity, boolean incrementAttempts, Role doneBy) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Score variable can't be updated in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserUserComment(java.lang.String,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserUserComment(String userComment, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Comment variable can't be updated in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public Integer getUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "No attempts available in ST nodes", null);

	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasAttemptsConfigured()
	 */
	@Override
	public boolean hasAttemptsConfigured() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#updateUserAttempts(java.lang.Integer,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.core.id.Identity)
	 */
	@Override
	public void updateUserAttempts(Integer userAttempts, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity, Role doneBy) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Attempts variable can't be updated in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#incrementUserAttempts(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public void incrementUserAttempts(UserCourseEnvironment userCourseEnvironment, Role doneBy) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Attempts variable can't be updated in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public Controller getDetailsEditController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Details controler not available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListView(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public String getDetailsListView(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Details not available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#getDetailsListViewHeaderKey()
	 */
	@Override
	public String getDetailsListViewHeaderKey() {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Details not available in ST nodes", null);
	}

	/**
	 * @see org.olat.course.nodes.AssessableCourseNode#hasDetails()
	 */
	@Override
	public boolean hasDetails() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.AbstractAccessableCourseNode#getConditionExpressions()
	 */
	@Override
	public List getConditionExpressions() {
		ArrayList retVal;
		List parentsConditions = super.getConditionExpressions();
		if (parentsConditions.size() > 0) {
			retVal = new ArrayList(parentsConditions);
		} else {
			retVal = new ArrayList();
		}
		// init passedExpression and scoreExpression
		getScoreCalculator();
		//
		passedExpression.setExpertMode(true);
		String coS = passedExpression.getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(passedExpression.getConditionId());
			ce.setExpressionString(passedExpression.getConditionExpression());
			retVal.add(ce);
		}
		scoreExpression.setExpertMode(true);
		coS = scoreExpression.getConditionExpression();
		if (coS != null && !coS.equals("")) {
			// an active condition is defined
			ConditionExpression ce = new ConditionExpression(scoreExpression.getConditionId());
			ce.setExpressionString(scoreExpression.getConditionExpression());
			retVal.add(ce);
		}
		//
		return retVal;
	}

	@Override
	public Controller getDetailsEditController(UserRequest ureq,
			WindowControl wControl, BreadcrumbPanel stackPanel,
			UserCourseEnvironment coachCourseEnv, UserCourseEnvironment assessedUserCourseEnvironment) {
		// TODO Auto-generated method stub
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Details controler not available in ST nodes", null);

	}

	@Override
	public TabbableController createEditController(UserRequest ureq,
			WindowControl wControl, BreadcrumbPanel stackPanel,
			ICourse course, UserCourseEnvironment euce) { 
		// TODO Auto-generated method stub
		updateModuleConfigDefaults(false);
		// only the precondition "access" can be configured till now
		BBautOLATStructureNodeEditController childTabCntrllr;
		childTabCntrllr = new BBautOLATStructureNodeEditController(ureq, wControl, this, course.getCourseFolderContainer(), 
				course.getCourseEnvironment().getCourseGroupManager(), course.getEditorTreeModel(), euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, childTabCntrllr);
	}

	@Override
	public boolean isAssessedBusinessGroups() {
		return false;
	}

	@Override
	public boolean hasIndividualAsssessmentDocuments() {
		return false;
	}

	@Override
	public List<File> getIndividualAssessmentDocuments(UserCourseEnvironment userCourseEnvironment) {
		return Collections.emptyList();
	}

	@Override
	public void addIndividualAssessmentDocument(File document, String filename, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		// individual assessment documents not supported
	}

	@Override
	public void removeIndividualAssessmentDocument(File document, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		// individual assessment documents not supported
	}

	@Override
	public void updateLastModifications(UserCourseEnvironment userCourseEnvironment, Identity identity, Role doneBy) {
		// not supported (cannot be manually assessed)
	}

	@Override
	public boolean hasCompletion() {
		return false;
	}

	@Override
	public Double getUserCurrentRunCompletion(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "No completion available for autolat structure nodes", null);
	}

	@Override
	public AssessmentCourseNodeController getIdentityListController(UserRequest ureq, WindowControl wControl,
			TooledStackedPanel stackPanel, RepositoryEntry courseEntry, BusinessGroup group,
			UserCourseEnvironment coachCourseEnv, AssessmentToolContainer toolContainer,
			AssessmentToolSecurityCallback assessmentCallback) {
		return new IdentityListCourseNodeController(ureq, wControl, stackPanel,
				courseEntry, group, this, coachCourseEnv, toolContainer, assessmentCallback);
	}

	@Override
	public void updateCurrentCompletion(UserCourseEnvironment userCourseEnvironment, Identity identity,
			Double currentCompletion, AssessmentRunStatus status, Role doneBy) {
		throw new OLATRuntimeException(BBautOLATStructureNode.class, "Completion variable can't be updated in autolat structure nodes", null);
	}
}