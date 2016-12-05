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
package org.olat.modules.coach.ui;

import java.util.Collections;
import java.util.List;

import org.olat.basesecurity.BaseSecurity;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.stack.TooledController;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.course.CorruptedCourseException;
import org.olat.course.assessment.EfficiencyStatement;
import org.olat.course.assessment.UserEfficiencyStatement;
import org.olat.course.assessment.manager.EfficiencyStatementManager;
import org.olat.course.assessment.ui.tool.AssessmentIdentityCourseController;
import org.olat.course.certificate.ui.CertificateAndEfficiencyStatementController;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.modules.assessment.ui.event.AssessmentFormEvent;
import org.olat.modules.coach.model.EfficiencyStatementEntry;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  9 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EfficiencyStatementDetailsController extends BasicController implements Activateable2, TooledController {
	
	private TooledStackedPanel stackPanel;
	private final VelocityContainer mainVC;
	private SegmentViewComponent segmentView;
	private Link assessmentLink,  efficiencyStatementLink;

	private String details;
	private int entryIndex,  numOfEntries;
	private Link previousLink, detailsCmp, nextLink;
	
	private boolean hasChanged;
	private EfficiencyStatementEntry statementEntry;
	private CertificateAndEfficiencyStatementController statementCtrl;
	private AssessmentIdentityCourseController assessmentCtrl;
	
	private final Identity assessedIdentity;
	

	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private EfficiencyStatementManager efficiencyStatementManager;

	public EfficiencyStatementDetailsController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			EfficiencyStatementEntry statementEntry, Identity assessedIdentity, String details, int entryIndex, int numOfEntries, boolean selectAssessmentTool) {
		super(ureq, wControl);
		
		this.details = details;
		this.entryIndex = entryIndex;
		this.stackPanel = stackPanel;
		this.numOfEntries = numOfEntries;
		this.statementEntry = statementEntry;
		
		mainVC = createVelocityContainer("efficiency_details");

		RepositoryEntry entry = statementEntry.getCourse();
		if(assessedIdentity == null) {
			this.assessedIdentity = securityManager.loadIdentityByKey(statementEntry.getIdentityKey());
		} else {
			this.assessedIdentity = assessedIdentity;
		}
		statementCtrl = createEfficiencyStatementController(ureq);
		listenTo(statementCtrl);
		
		if(entry == null) {
			mainVC.put("segmentCmp", statementCtrl.getInitialComponent());
		} else {
			try {
				UserCourseEnvironment coachCourseEnv = loadUserCourseEnvironment(ureq, entry);
				assessmentCtrl = new AssessmentIdentityCourseController(ureq, wControl, stackPanel, entry, coachCourseEnv, assessedIdentity);
				listenTo(assessmentCtrl);
				
				segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
				efficiencyStatementLink = LinkFactory.createLink("details.statement", mainVC, this);
				segmentView.addSegment(efficiencyStatementLink, !selectAssessmentTool);
				
				assessmentLink = LinkFactory.createLink("details.assessment", mainVC, this);
				segmentView.addSegment(assessmentLink, selectAssessmentTool);
				
				if(selectAssessmentTool) {
					mainVC.put("segmentCmp", assessmentCtrl.getInitialComponent());
				} else {
					mainVC.put("segmentCmp", statementCtrl.getInitialComponent());
				}
			} catch(CorruptedCourseException e) {
				logError("", e);
			}
		}

		putInitialPanel(mainVC);
	}
	
	private UserCourseEnvironment loadUserCourseEnvironment(UserRequest ureq, RepositoryEntry entry) {
		RepositoryEntrySecurity reSecurity = repositoryManager.isAllowed(ureq, entry);
		return new UserCourseEnvironmentImpl(ureq.getUserSession().getIdentityEnvironment(), null, getWindowControl(),
				null, null, null,
				reSecurity.isCourseCoach() || reSecurity.isGroupCoach(),
				reSecurity.isEntryAdmin(),
				reSecurity.isCourseParticipant() || reSecurity.isGroupParticipant(),
				reSecurity.isReadOnly());
	}

	@Override
	public void initTools() {
		previousLink = LinkFactory.createToolLink("previous", translate("previous"), this);
		previousLink.setIconLeftCSS("o_icon o_icon_previous");
		previousLink.setEnabled(entryIndex > 0);
		stackPanel.addTool(previousLink);

		detailsCmp = LinkFactory.createToolLink("details.course", StringHelper.escapeHtml(details), this);
		detailsCmp.setIconLeftCSS("o_icon o_icon_user");
		stackPanel.addTool(detailsCmp);

		nextLink = LinkFactory.createToolLink("next", translate("next"), this);
		nextLink.setIconLeftCSS("o_icon o_icon_next");
		nextLink.setEnabled(entryIndex < numOfEntries);
		stackPanel.addTool(nextLink);
		stackPanel.addListener(this);
	}

	public EfficiencyStatementEntry getEntry() {
		return statementEntry;
	}
	
	public boolean isAssessmentToolSelected() {
		return assessmentCtrl != null && assessmentCtrl.getInitialComponent() == mainVC.getComponent("segmentCmp"); 
	}
	
	@Override
	protected void doDispose() {
		stackPanel.removeListener(this);
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(assessmentCtrl == source) {
			if(event == Event.CHANGED_EVENT || event instanceof AssessmentFormEvent) {
				//reload the details
				efficiencyStatementChanged();
				hasChanged = true;
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(nextLink == source || previousLink == source) {
			fireEvent(ureq, event);
		} else if(source == segmentView && event instanceof SegmentViewEvent) {
			SegmentViewEvent sve = (SegmentViewEvent)event;
			if(efficiencyStatementLink != null && efficiencyStatementLink.getComponentName().equals(sve.getComponentName())) {
				if(hasChanged) {
					//reload
					removeAsListenerAndDispose(statementCtrl);
					statementCtrl = createEfficiencyStatementController(ureq);
					listenTo(statementCtrl);
					hasChanged = false;
				}
				mainVC.put("segmentCmp", statementCtrl.getInitialComponent());
			} else if(assessmentLink != null && assessmentLink.getComponentName().equals(sve.getComponentName())) {
				mainVC.put("segmentCmp", assessmentCtrl.getInitialComponent());
			}
		}
	}
	
	private CertificateAndEfficiencyStatementController createEfficiencyStatementController(UserRequest ureq) {
		RepositoryEntry entry = statementEntry.getCourse();
		UserEfficiencyStatement statement = statementEntry.getUserEfficencyStatement();
		EfficiencyStatement efficiencyStatement = null;
		if(statement != null) {
			RepositoryEntry re = statementEntry.getCourse();
			efficiencyStatement = efficiencyStatementManager.getUserEfficiencyStatementByCourseRepositoryEntry(re, assessedIdentity);
		}
		return new CertificateAndEfficiencyStatementController(getWindowControl(), ureq, assessedIdentity, null, entry.getOlatResource().getKey(), entry, efficiencyStatement, true);
	}
	
	private void efficiencyStatementChanged() {
		List<Identity> assessedIdentityList = Collections.singletonList(assessedIdentity);
		RepositoryEntry re = statementEntry.getCourse();
		efficiencyStatementManager.updateEfficiencyStatements(re, assessedIdentityList);
	}
}