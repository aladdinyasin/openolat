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
package org.olat.modules.qpool.ui.metadata;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.Util;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemSecurityCallback;
import org.olat.modules.qpool.ui.QuestionsController;
import org.olat.modules.qpool.ui.events.QItemEdited;

/**
 * 
 * Initial date: 24.01.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MetadatasController extends BasicController {

	private final VelocityContainer mainVC;
	private GeneralMetadataEditController generalEditCtrl;
	private QuestionMetadataEditController questionEditCtrl;
	private RightsMetadataEditController rightsEditCtrl;
	private TechnicalMetadataEditController technicalEditCtrl;
	private PoolsMetadataController poolsCtrl;
	private SharesMetadataController sharesController;
	
	private QuestionItem item;
	
	public MetadatasController(UserRequest ureq, WindowControl wControl, QuestionItem item, QuestionItemSecurityCallback securityCallback) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(QuestionsController.class, getLocale(), getTranslator()));
		
		this.item = item;

		mainVC = createVelocityContainer("item_metadatas");
		generalEditCtrl = new GeneralMetadataEditController(ureq, wControl, item, securityCallback);
		listenTo(generalEditCtrl);
		mainVC.put("details_general", generalEditCtrl.getInitialComponent());

		questionEditCtrl = new QuestionMetadataEditController(ureq, wControl, item, securityCallback);
		listenTo(questionEditCtrl);
		mainVC.put("details_question", questionEditCtrl.getInitialComponent());
		
		rightsEditCtrl = new RightsMetadataEditController(ureq, wControl, item, securityCallback);
		listenTo(rightsEditCtrl);
		mainVC.put("details_rights", rightsEditCtrl.getInitialComponent());

		technicalEditCtrl = new TechnicalMetadataEditController(ureq, wControl, item, securityCallback);
		listenTo(technicalEditCtrl);
		mainVC.put("details_technical", technicalEditCtrl.getInitialComponent());

		poolsCtrl = new PoolsMetadataController(ureq, wControl, item);
		mainVC.put("details_pools", poolsCtrl.getInitialComponent());
		
		sharesController = new SharesMetadataController(ureq, wControl, item);
		mainVC.put("details_shares", sharesController.getInitialComponent());

		putInitialPanel(mainVC);
	}

	@Override
	protected void doDispose() {
		removeAsListenerAndDispose(generalEditCtrl);
		removeAsListenerAndDispose(questionEditCtrl);
		removeAsListenerAndDispose(rightsEditCtrl);
		removeAsListenerAndDispose(technicalEditCtrl);
		removeAsListenerAndDispose(poolsCtrl);
		generalEditCtrl = null;
		questionEditCtrl = null;
		rightsEditCtrl = null;
		technicalEditCtrl = null;
		poolsCtrl = null;
	}
	
	public QuestionItem getItem() {
		return item;
	}
	
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		// QPoolEvent.EDIT löschen?
		
//		if(QPoolEvent.EDIT.endsWith(event.getCommand())) {
//			if(securityCallback.canEditMetadata()) {
//				if(source == generalCtrl) {
//					doEditGeneralMetadata(ureq);
//				} else if(source == educationalCtrl) {
//					doEditEducationalMetadata(ureq);
//				} else if(source == questionCtrl) {
//					doEditQuestionMetadata(ureq);
//				} else if(source == technicalCtrl) {
//					doEditTechnicalMetadata(ureq);
//				} else if(source == rightsCtrl) {
//					doEditRightsMetadata(ureq);
//				}
//			} else if (securityCallback.canEditLifecycle()) {
//				if(source == lifecycleCtrl) {
//					doEditLifecycleMetadata(ureq);
//				}
//			}
//		} else 
		if(event instanceof QItemEdited) {
			QItemEdited editEvent = (QItemEdited)event;
			//TODO uh nedded
			reloadData(editEvent.getItem());
			fireEvent(ureq, editEvent);
		}
	}
	
	
	// TODO uh needed?
	public void updateShares() {
		poolsCtrl.setItem(getItem());
	}
	
	public void reloadData(QuestionItem reloadedItem) {
//		this.item = reloadedItem;
//		generalEditCtrl.setItem(reloadedItem);
//		questionEditCtrl.setItem(reloadedItem);
//		rightsEditCtrl.setItem(reloadedItem);
//		technicalEditCtrl.setItem(reloadedItem);
		// TODO uh auch mit pool/shares
	}
}