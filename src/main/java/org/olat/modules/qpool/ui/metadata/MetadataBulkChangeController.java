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

import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.getQItemTypeKeyValues;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.getQLicenseKeyValues;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.toBigDecimal;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.toInt;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.validateBigDecimal;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.validateElementLogic;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.validateRights;
import static org.olat.modules.qpool.ui.metadata.MetaUIFactory.validateSelection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Util;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.manager.MetadataConverterHelper;
import org.olat.modules.qpool.model.QEducationalContext;
import org.olat.modules.qpool.model.QuestionItemImpl;
import org.olat.modules.qpool.ui.QuestionsController;
import org.olat.modules.qpool.ui.metadata.MetaUIFactory.KeyValues;
import org.olat.modules.qpool.ui.tree.QPoolTaxonomyTreeBuilder;
import org.olat.modules.taxonomy.TaxonomyLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 02.05.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MetadataBulkChangeController extends FormBasicController {

	private static final String[] EMPTY_VALUES = new String[]{ "" };
	
	private TextElement topicEl, keywordsEl, coverageEl, addInfosEl, languageEl;
	private SingleSelection taxonomyLevelEl;
	private SingleSelection contextEl;
	private FormLayoutContainer learningTimeContainer;
	private IntegerElement learningTimeDayElement, learningTimeHourElement, learningTimeMinuteElement, learningTimeSecondElement;
	private SingleSelection typeEl, assessmentTypeEl;
	private TextElement difficultyEl, stdevDifficultyEl, differentiationEl, numAnswerAltEl;
	private TextElement versionEl;
	private SingleSelection statusEl;
	private KeyValues licenseKeys;
	private SingleSelection copyrightEl;
	private TextElement descriptionEl;
	private FormLayoutContainer rightsWrapperCont;

	private Map<MultipleSelectionElement, FormLayoutContainer> checkboxContainer = new HashMap<>();
	private final List<MultipleSelectionElement> checkboxSwitch = new ArrayList<>();
	
	private List<QuestionItem> updatedItems;
	private final List<QuestionItemShort> items;
	
	@Autowired
	private QPoolService qpoolService;
	@Autowired
	private QPoolTaxonomyTreeBuilder qpoolTaxonomyTreeBuilder;
	
	public MetadataBulkChangeController(UserRequest ureq, WindowControl wControl, List<QuestionItemShort> items) {
		super(ureq, wControl, "bulk_change");
		setTranslator(Util.createPackageTranslator(QuestionsController.class, getLocale(), getTranslator()));
		this.items = items;
		initForm(ureq);
	}
	
	public List<QuestionItem> getUpdatedItems() {
		return updatedItems == null ? Collections.<QuestionItem>emptyList() : updatedItems;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormDescription("bulk.change.description");
		
		initGeneralForm(formLayout);
		initQuestionForm(formLayout);
		initTechnicalForm(formLayout);
		initRightsForm(formLayout);

		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonsCont.setRootForm(mainForm);
		formLayout.add(buttonsCont);
		uifactory.addFormSubmitButton("ok", "ok", buttonsCont);
		uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
	}
	
	private void initGeneralForm(FormItemContainer formLayout) {
		FormLayoutContainer generalCont = FormLayoutContainer.createDefaultFormLayout("general", getTranslator());
		generalCont.setRootForm(mainForm);
		formLayout.add(generalCont);
		
		topicEl = uifactory.addTextElement("general.topic", "general.topic", 1000, null, generalCont);
		decorate(topicEl, generalCont);
		
		qpoolTaxonomyTreeBuilder.loadTaxonomyLevelsSelection(getIdentity(), false);
		taxonomyLevelEl = uifactory.addDropdownSingleselect("classification.taxonomic.path", generalCont,
				qpoolTaxonomyTreeBuilder.getSelectableKeys(), qpoolTaxonomyTreeBuilder.getSelectableValues(), null);
		decorate(taxonomyLevelEl, generalCont);
		
		KeyValues contexts = MetaUIFactory.getContextKeyValues(getTranslator(), qpoolService);
		contextEl = uifactory.addDropdownSingleselect("educational.context", "educational.context", generalCont,
				contexts.getKeys(), contexts.getValues(), null);
		decorate(contextEl, generalCont);
		
		keywordsEl = uifactory.addTextElement("general.keywords", "general.keywords", 1000, null, generalCont);
		decorate(keywordsEl, generalCont);
		
		addInfosEl = uifactory.addTextElement("general.additional.informations", "general.additional.informations.long",
				256, "", generalCont);
		decorate(addInfosEl, generalCont);
		
		coverageEl = uifactory.addTextElement("general.coverage", "general.coverage", 1000, null, generalCont);
		decorate(coverageEl, generalCont);
		
		languageEl = uifactory.addTextElement("general.language", "general.language", 10, "", generalCont);
		decorate(languageEl, generalCont);
		
		KeyValues types = MetaUIFactory.getAssessmentTypes(getTranslator());
		assessmentTypeEl = uifactory.addDropdownSingleselect("question.assessmentType", "question.assessmentType", generalCont,
				types.getKeys(), types.getValues(), null);
		decorate(assessmentTypeEl, generalCont);
	}
	
	private void initQuestionForm(FormItemContainer formLayout) {
		FormLayoutContainer questionCont = FormLayoutContainer.createDefaultFormLayout("question", getTranslator());
		questionCont.setRootForm(mainForm);
		formLayout.add(questionCont);
		
		KeyValues typeKeys = getQItemTypeKeyValues(getTranslator(), qpoolService);
		typeEl = uifactory.addDropdownSingleselect("question.type", "question.type", questionCont, typeKeys.getKeys(), typeKeys.getValues(), null);
		decorate(typeEl, questionCont);
		
		String page = velocity_root + "/learning_time.html";
		learningTimeContainer = FormLayoutContainer.createCustomFormLayout("educational.learningTime", getTranslator(), page);
		learningTimeContainer.setRootForm(mainForm);
		questionCont.add(learningTimeContainer);
		decorate(learningTimeContainer, questionCont);

		learningTimeDayElement = uifactory.addIntegerElement("learningTime.day", "", 0, learningTimeContainer);
		learningTimeDayElement.setDisplaySize(3);
		learningTimeHourElement = uifactory.addIntegerElement("learningTime.hour", "", 0, learningTimeContainer);
		learningTimeHourElement.setDisplaySize(3);
		learningTimeMinuteElement = uifactory.addIntegerElement("learningTime.minute", "", 0, learningTimeContainer);
		learningTimeMinuteElement.setDisplaySize(3);
		learningTimeSecondElement = uifactory.addIntegerElement("learningTime.second", "", 0, learningTimeContainer);
		learningTimeSecondElement.setDisplaySize(3);
		
		difficultyEl = uifactory.addTextElement("question.difficulty", "question.difficulty", 10, null, questionCont);
		difficultyEl.setExampleKey("question.difficulty.example", null);
		difficultyEl.setDisplaySize(4);
		decorate(difficultyEl, questionCont);

		stdevDifficultyEl = uifactory.addTextElement("question.stdevDifficulty", "question.stdevDifficulty", 10, null, questionCont);
		stdevDifficultyEl.setExampleKey("question.stdevDifficulty.example", null);
		stdevDifficultyEl.setDisplaySize(4);
		decorate(stdevDifficultyEl, questionCont);
		
		differentiationEl = uifactory.addTextElement("question.differentiation", "question.differentiation", 10, null, questionCont);
		differentiationEl.setExampleKey("question.differentiation.example", null);
		differentiationEl.setDisplaySize(4);
		decorate(differentiationEl, questionCont);
		
		numAnswerAltEl = uifactory.addTextElement("question.numOfAnswerAlternatives", "question.numOfAnswerAlternatives", 10, null, questionCont);
		numAnswerAltEl.setDisplaySize(4);
		decorate(numAnswerAltEl, questionCont);

	}

	private void initTechnicalForm(FormItemContainer formLayout) {
		FormLayoutContainer technicalCont = FormLayoutContainer.createDefaultFormLayout("technical", getTranslator());
		technicalCont.setRootForm(mainForm);
		formLayout.add(technicalCont);
		
		versionEl = uifactory.addTextElement("lifecycle.version", "lifecycle.version", 50, null, technicalCont);
		decorate(versionEl, technicalCont);
		
		KeyValues status = MetaUIFactory.getStatus(getTranslator());
		statusEl = uifactory.addDropdownSingleselect("lifecycle.status", "lifecycle.status", technicalCont,
				status.getKeys(), status.getValues(), null);
		decorate(statusEl, technicalCont);	;
	}
	
	private void initRightsForm(FormItemContainer formLayout) {
		FormLayoutContainer rightsCont = FormLayoutContainer.createDefaultFormLayout("rights", getTranslator());
		rightsCont.setRootForm(mainForm);
		formLayout.add(rightsCont);

		rightsWrapperCont = FormLayoutContainer.createDefaultFormLayout("rights.copyright", getTranslator());
		rightsWrapperCont.setRootForm(mainForm);
		rightsCont.add(rightsWrapperCont);
		decorate(rightsWrapperCont, rightsCont);

		licenseKeys = getQLicenseKeyValues(qpoolService);
		copyrightEl = uifactory.addDropdownSingleselect("rights.copyright.sel", "rights.copyright", rightsWrapperCont,
				licenseKeys.getKeys(), licenseKeys.getValues(), null);
		copyrightEl.addActionListener(FormEvent.ONCHANGE);
		copyrightEl.select("none", true);

		descriptionEl = uifactory.addTextAreaElement("rights.description", "rights.description", 1000, 6, 40, true, null, rightsWrapperCont);
	}
	
	private FormItem decorate(FormItem item, FormLayoutContainer formLayout) {
		String itemName = item.getName();
		MultipleSelectionElement checkbox = uifactory.addCheckboxesHorizontal("cbx_" + itemName, itemName, formLayout, new String[] { itemName }, EMPTY_VALUES);
		checkbox.select(itemName, false);
		checkbox.addActionListener(FormEvent.ONCLICK);
		checkbox.setUserObject(item);
		checkboxSwitch.add(checkbox);

		item.setLabel(null, null);
		item.setVisible(false);
		item.setUserObject(checkbox);
		
		checkboxContainer.put(checkbox, formLayout);
		formLayout.moveBefore(checkbox, item);
		return item;
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(checkboxSwitch.contains(source)) {
			MultipleSelectionElement checkbox = (MultipleSelectionElement)source;
			FormItem item = (FormItem)checkbox.getUserObject();
			item.setVisible(checkbox.isAtLeastSelected(1));
			checkboxContainer.get(checkbox).setDirty(true);
		} else if(copyrightEl == source) {
			descriptionEl.setVisible(copyrightEl.isVisible() && copyrightEl.getSelectedKey().equals(licenseKeys.getLastKey()));
			rightsWrapperCont.setDirty(true);
		} else {
			super.formInnerEvent(ureq, source, event);
		}
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		//general
		allOk &= validateElementLogic(topicEl, topicEl.getMaxLength(), true, isEnabled(topicEl));
		allOk &= validateElementLogic(keywordsEl, keywordsEl.getMaxLength(), false, isEnabled(keywordsEl));
		allOk &= validateElementLogic(coverageEl, coverageEl.getMaxLength(), false, isEnabled(coverageEl));
		allOk &= validateElementLogic(addInfosEl, addInfosEl.getMaxLength(), false, isEnabled(addInfosEl));
		allOk &= validateElementLogic(languageEl, languageEl.getMaxLength(), true, isEnabled(languageEl));
		
		//question
		allOk &= validateBigDecimal(difficultyEl, 0.0d, 1.0d, true);
		allOk &= validateBigDecimal(stdevDifficultyEl, 0.0d, 1.0d, true);
		allOk &= validateBigDecimal(differentiationEl, -1.0d, 1.0d, true);
		
		//technical
		allOk &= validateElementLogic(versionEl, versionEl.getMaxLength(), true, isEnabled(versionEl));
		allOk &= validateSelection(statusEl, isEnabled(statusEl));
		
		//rights
		allOk &= validateRights(copyrightEl, descriptionEl, licenseKeys, isEnabled(rightsWrapperCont));
		
		return allOk & super.validateFormLogic(ureq);
	}
	
	private boolean isEnabled(FormItem item) {
		return ((MultipleSelectionElement)item.getUserObject()).isAtLeastSelected(1);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		updatedItems = new ArrayList<>();
		for(QuestionItemShort item : items) {
			QuestionItem fullItem = qpoolService.loadItemById(item.getKey());
			if(fullItem instanceof QuestionItemImpl) {
				QuestionItemImpl itemImpl = (QuestionItemImpl)fullItem;
				formOKGeneral(itemImpl);
				formOKQuestion(itemImpl);
				formOKTechnical(itemImpl);
				if(isEnabled(rightsWrapperCont)) {
					RightsMetadataEditController.formOKRights(itemImpl, copyrightEl, descriptionEl, licenseKeys, qpoolService);
				}
				QuestionItem merged = qpoolService.updateItem(itemImpl);
				updatedItems.add(merged);
			}
		}
		fireEvent(ureq, Event.DONE_EVENT);
	}
	
	private void formOKGeneral(QuestionItemImpl itemImpl) {
		if(isEnabled(topicEl))
			itemImpl.setTopic(topicEl.getValue());
		if(isEnabled(keywordsEl))
			itemImpl.setKeywords(keywordsEl.getValue());
		if(isEnabled(coverageEl))
			itemImpl.setCoverage(coverageEl.getValue());
		if(isEnabled(addInfosEl))
			itemImpl.setAdditionalInformations(addInfosEl.getValue());
		if(isEnabled(languageEl))
			itemImpl.setLanguage(languageEl.getValue());
		if(isEnabled(taxonomyLevelEl)) {
			String selectedKey = taxonomyLevelEl.getSelectedKey();
			TaxonomyLevel taxonomyLevel = qpoolTaxonomyTreeBuilder.getTaxonomyLevel(selectedKey);
			itemImpl.setTaxonomyLevel(taxonomyLevel);
		}
	}
	
	private void formOKQuestion(QuestionItemImpl itemImpl) {
		if(isEnabled(typeEl) && typeEl.isOneSelected()) {
			String typeKey = typeEl.getSelectedKey();
			itemImpl.setType(qpoolService.getItemType(typeKey));
		}
		
		if(isEnabled(contextEl)) {
			if(contextEl.isOneSelected()) {
				QEducationalContext context = qpoolService.getEducationlContextByLevel(contextEl.getSelectedKey());
				itemImpl.setEducationalContext(context);
			} else {
				itemImpl.setEducationalContext(null);
			}
		}
		
		if(isEnabled(learningTimeContainer)) {
			int day = learningTimeDayElement.getIntValue();
			int hour = learningTimeHourElement.getIntValue();
			int minute = learningTimeMinuteElement.getIntValue();
			int seconds = learningTimeSecondElement.getIntValue();
			String timeStr = MetadataConverterHelper.convertDuration(day, hour, minute, seconds);
			itemImpl.setEducationalLearningTime(timeStr);
		}
		
		if(isEnabled(difficultyEl))
			itemImpl.setDifficulty(toBigDecimal(difficultyEl.getValue()));
		if(isEnabled(stdevDifficultyEl))
			itemImpl.setStdevDifficulty(toBigDecimal(stdevDifficultyEl.getValue()));
		if(isEnabled(differentiationEl))
			itemImpl.setDifferentiation(toBigDecimal(differentiationEl.getValue()));
		if(isEnabled(numAnswerAltEl))
			itemImpl.setNumOfAnswerAlternatives(toInt(numAnswerAltEl.getValue()));
		if(isEnabled(typeEl)) {
			String assessmentType = assessmentTypeEl.isOneSelected() ? assessmentTypeEl.getSelectedKey() : null;
			itemImpl.setAssessmentType(assessmentType);
		}
	}

	private void formOKTechnical(QuestionItemImpl itemImpl) {
		if(isEnabled(statusEl) && statusEl.isOneSelected())
			itemImpl.setStatus(statusEl.getSelectedKey());
		if(isEnabled(versionEl))
			itemImpl.setItemVersion(versionEl.getValue());
	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}