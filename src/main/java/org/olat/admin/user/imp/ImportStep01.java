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
package org.olat.admin.user.imp;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.AbstractCSSIconFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelImpl;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.BasicStep;
import org.olat.core.gui.control.generic.wizard.PrevNextFinishConfig;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepFormController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

class ImportStep01 extends BasicStep {
	private static final String usageIdentifyer = UserImportController.class.getCanonicalName();

	private boolean newUsers;
	private boolean canCreateOLATPassword;

	public ImportStep01(UserRequest ureq, boolean canCreateOLATPassword, boolean newUsers) {
		super(ureq);
		this.newUsers = newUsers;
		this.canCreateOLATPassword = canCreateOLATPassword;
		setI18nTitleAndDescr("step1.description", "step1.short.description");
		setNextStep(new ImportStep02(ureq));
	}

	@Override
	public PrevNextFinishConfig getInitialPrevNextFinishConfig() {
		return new PrevNextFinishConfig(true, true, true);
	}

	@Override
	public StepFormController getStepController(UserRequest ureq, WindowControl windowControl, StepsRunContext stepsRunContext, Form form) {
		StepFormController stepI = new ImportStepForm01(ureq, windowControl, form, stepsRunContext);
		return stepI;
	}

	private final class ImportStepForm01 extends StepFormBasicController {

		private FormLayoutContainer textContainer;
		private MultipleSelectionElement updateEl;
		private MultipleSelectionElement updatePasswordEl;
		private List<UserPropertyHandler> userPropertyHandlers;

		public ImportStepForm01(UserRequest ureq, WindowControl control, Form rootForm, StepsRunContext runContext) {
			super(ureq, control, rootForm, runContext, LAYOUT_VERTICAL, null);
			// use custom translator with fallback to user properties translator
			UserManager um = UserManager.getInstance();
			setTranslator(um.getPropertyHandlerTranslator(getTranslator()));
			flc.setTranslator(getTranslator());
			initForm(ureq);
		}

		@Override
		protected void doDispose() {
			//
		}

		@Override
		protected void formOK(UserRequest ureq) {
			Boolean updateUsers = Boolean.FALSE;
			if(updateEl != null && updateEl.isAtLeastSelected(1)) {
				updateUsers = Boolean.TRUE; 
			}
			addToRunContext("updateUsers", updateUsers);
			
			Boolean updatePasswords = Boolean.FALSE;
			if(updatePasswordEl != null && updatePasswordEl.isAtLeastSelected(1)) {
				updatePasswords = Boolean.TRUE; 
			}
			addToRunContext("updatePasswords", updatePasswords);
			fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
		}

		@Override
		protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
			FormLayoutContainer formLayoutVertical = FormLayoutContainer.createVerticalFormLayout("vertical", getTranslator());
			formLayout.add(formLayoutVertical);

			@SuppressWarnings("unchecked")
			List<Identity> idents = (List<Identity>) getFromRunContext("idents");
			@SuppressWarnings("unchecked")
			List<UpdateIdentity> updateIdents = (List<UpdateIdentity>) getFromRunContext("updateIdents");
			@SuppressWarnings("unchecked")
			List<TransientIdentity> newIdents = (List<TransientIdentity>) getFromRunContext("newIdents");
			textContainer = FormLayoutContainer.createCustomFormLayout("step1", getTranslator(), velocity_root + "/step1.html");
			formLayoutVertical.add(textContainer);

			int cntall = idents.size();
			int cntNew = newIdents.size();
			int cntOld = cntall - cntNew;
			textContainer.contextPut("newusers", newUsers);
			String overview = getTranslator().translate("import.confirm", new String[] { "" + cntall, "" + cntNew, "" + cntOld });
			textContainer.contextPut("overview", overview);
			textContainer.contextPut("updateusers", updateIdents.isEmpty());
			if(!updateIdents.isEmpty()) {
				String[] updateValues = new String[]{ translate("update.user") };
				updateEl = uifactory
						.addCheckboxesHorizontal("update.user", textContainer, new String[]{"on"}, updateValues);
				updateEl.select("on", true);
				
				if(canCreateOLATPassword) {
					String[] theValues = new String[]{ translate("update.password") };
					updatePasswordEl = uifactory
							.addCheckboxesHorizontal("update.password", textContainer, new String[]{"on"}, theValues);
				}
			}

			FlexiTableColumnModel tableColumnModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
			int colPos = 0;
			// add special column with information about whether this user
			// exists already or not
			FlexiColumnModel newUserCustomColumnModel = new DefaultFlexiColumnModel("table.user.existing", colPos++);
			newUserCustomColumnModel.setCellRenderer(new UserNewOldCustomFlexiCellRenderer());
			newUserCustomColumnModel.setAlignment(FlexiColumnModel.ALIGNMENT_CENTER);
			tableColumnModel.addFlexiColumnModel(newUserCustomColumnModel);
			
			// fixed fields:
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.user.login", colPos++));
			if (canCreateOLATPassword) {
				tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.user.pwd", colPos++));
			}
			tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel("table.user.lang", colPos++));

			// followed by all properties configured
			// if only mandatory required: check for um.isMandatoryUserProperty(usageIdentifyer, userPropertyHandler);
			userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, true);
			for (int i = 0; i < userPropertyHandlers.size(); i++) {
				UserPropertyHandler userPropertyHandler = userPropertyHandlers.get(i);
					tableColumnModel.addFlexiColumnModel(new DefaultFlexiColumnModel(userPropertyHandler.i18nColumnDescriptorLabelKey(), colPos++));
			}

			FlexiTableDataModel<Identity> tableDataModel = new FlexiTableDataModelImpl<Identity>(new Model(idents, colPos), tableColumnModel);
			uifactory.addTableElement(getWindowControl(), "newUsers", tableDataModel, getTranslator(), formLayoutVertical);
		}
	}
}

/**
 * 
 * Description:<br>
 * Special cell renderer that uses a css class icon to display the new user type
 * 
 * <P>
 * Initial Date:  21.03.2008 <br>
 * @author gnaegi
 */
class UserNewOldCustomFlexiCellRenderer extends AbstractCSSIconFlexiCellRenderer {

	@Override
	protected String getCellValue(Object cellValue) {
		return "";
	}

	@Override
	protected String getCssClass(Object cellValue) {
		if (cellValue instanceof Boolean) {
			if (((Boolean) cellValue).booleanValue()) {
				return "o_icon_new";
			} else {
				return "o_icon_warn";
			}
		}
		return "o_icon_error";
	}

	@Override
	protected String getHoverText(Object cellValue, Translator translator) {
		if (cellValue instanceof Boolean) {
			if (((Boolean) cellValue).booleanValue()) {
				return translator.translate("import.user.new.alt");
			} else {
				return translator.translate("import.user.existing.alt");
			}
		}
		return translator.translate("error");
	}
}