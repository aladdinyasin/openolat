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
package org.olat.modules.portfolio.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.OLATResourceable;
import org.olat.modules.portfolio.AssessmentSection;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.BinderConfiguration;
import org.olat.modules.portfolio.BinderSecurityCallback;
import org.olat.modules.portfolio.Category;
import org.olat.modules.portfolio.CategoryToElement;
import org.olat.modules.portfolio.Page;
import org.olat.modules.portfolio.Section;
import org.olat.modules.portfolio.model.PageRow;

/**
 * 
 * Initial date: 10.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SectionPageListController extends AbstractPageListController  {
	
	private Link newEntryLink;
	private CloseableModalController cmc;
	private PageMetadataEditController newPageCtrl;

	private final Binder binder;
	private final Section section;
	
	public SectionPageListController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			BinderSecurityCallback secCallback, Binder binder, BinderConfiguration config, Section section) {
		super(ureq, wControl, stackPanel, secCallback, config, "pages", true);
		this.binder = binder;
		this.section = section;
		
		initForm(ureq);
		loadModel();
	}
	
	@Override
	public void initTools() {
		if(secCallback.canAddPage()) {
			newEntryLink = LinkFactory.createToolLink("new.page", translate("create.new.page"), this);
			newEntryLink.setIconLeftCSS("o_icon o_icon-lg o_icon_new_portfolio");
			stackPanel.addTool(newEntryLink, Align.right);
		}
	}

	@Override
	protected void loadModel() {
		List<CategoryToElement> categorizedElements = portfolioService.getCategorizedSectionAndPages(section);
		Map<OLATResourceable,List<Category>> categorizedElementMap = new HashMap<>();
		Set<String> sectionAggregatedCategories = new HashSet<>();
		for(CategoryToElement categorizedElement:categorizedElements) {
			List<Category> categories = categorizedElementMap.get(categorizedElement.getCategorizedResource());
			if(categories == null) {
				categories = new ArrayList<>();
				categorizedElementMap.put(categorizedElement.getCategorizedResource(), categories);
			}
			categories.add(categorizedElement.getCategory());
			sectionAggregatedCategories.add(categorizedElement.getCategory().getName());
		}
		
		Map<Long,Long> numOfCommentsMap = portfolioService.getNumberOfComments(section);
		
		AssessmentSection assessmentSection = null;
		List<AssessmentSection> assessmentSections = portfolioService.getAssessmentSections(binder, getIdentity());
		for(AssessmentSection aSection:assessmentSections) {
			if(aSection.getSection().equals(section)) {
				assessmentSection = aSection;
			}
		}
		
		List<Page> pages = portfolioService.getPages(section);
		List<PageRow> rows = new ArrayList<>();
		boolean first = true;
		for (Page page : pages) {
			PageRow row = forgeRow(page, assessmentSection, first, categorizedElementMap, numOfCommentsMap);
			row.setSectionCategories(sectionAggregatedCategories);
			rows.add(row);
			first = false;
		}
		model.setObjects(rows);
		tableEl.reset();
		tableEl.reloadData();
	}
	
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(newEntryLink == source) {
			doCreateNewPage(ureq);
		}
		super.event(ureq, source, event);
	}
	
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if(newPageCtrl == source) {
			if(event == Event.DONE_EVENT) {
				loadModel();
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(newPageCtrl);
		removeAsListenerAndDispose(cmc);
		newPageCtrl = null;
		cmc = null;
	}
	
	private void doCreateNewPage(UserRequest ureq) {
		if(newPageCtrl != null) return;
		
		newPageCtrl = new PageMetadataEditController(ureq, getWindowControl(), binder, false, null, true);
		listenTo(newPageCtrl);
		
		String title = translate("create.new.page");
		cmc = new CloseableModalController(getWindowControl(), null, newPageCtrl.getInitialComponent(), true, title, true);
		listenTo(cmc);
		cmc.activate();
	}
}