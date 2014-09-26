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
package org.olat.repository.ui.list;

import java.util.List;

import org.olat.catalog.CatalogEntry;
import org.olat.catalog.CatalogManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.MainPanel;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.stack.BreadcrumbedStackedPanel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.model.SearchMyRepositoryEntryViewParams;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 28.01.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OverviewRepositoryListController extends BasicController implements Activateable2 {

	private final VelocityContainer mainVC;
	private final SegmentViewComponent segmentView;
	private final Link myCourseLink;
	private Link favoriteLink, catalogLink, searchCourseLink;
	
	private RepositoryEntryListController markedCtrl;
	private BreadcrumbedStackedPanel markedStackPanel;
	private RepositoryEntryListController myCoursesCtrl;
	private BreadcrumbedStackedPanel myCoursesStackPanel;
	private CatalogNodeController catalogCtrl;
	private BreadcrumbedStackedPanel catalogStackPanel;
	private RepositoryEntryListController searchCoursesCtrl;
	private BreadcrumbedStackedPanel searchCoursesStackPanel;
	
	private final boolean isGuestOnly;
	
	@Autowired
	private CatalogManager catalogManager;
	@Autowired
	private RepositoryModule repositoryModule;
	
	public OverviewRepositoryListController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, getLocale(), getTranslator()));
		isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();

		MainPanel mainPanel = new MainPanel("myCoursesMainPanel");
		mainPanel.setDomReplaceable(false);
		mainVC = createVelocityContainer("overview");
		mainPanel.setContent(mainVC);
		
		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
		segmentView.setReselect(true);
		if(!isGuestOnly) {
			favoriteLink = LinkFactory.createLink("search.mark", mainVC, this);
			favoriteLink.setElementCssClass("o_sel_mycourses_fav");
			segmentView.addSegment(favoriteLink, false);
		}
		
		myCourseLink = LinkFactory.createLink("search.mycourses.student", mainVC, this);
		myCourseLink.setElementCssClass("o_sel_mycourses_my");
		segmentView.addSegment(myCourseLink, false);
		
		if(repositoryModule.isCatalogEnabled() && repositoryModule.isCatalogBrowsingEnabled()) {
			catalogLink = LinkFactory.createLink("search.catalog", mainVC, this);
			catalogLink.setElementCssClass("o_sel_mycourses_catlog");
			segmentView.addSegment(catalogLink, false);
		}
		if(repositoryModule.isMyCoursesSearchEnabled()) {
			searchCourseLink = LinkFactory.createLink("search.courses.student", mainVC, this);
			searchCourseLink.setElementCssClass("o_sel_mycourses_search");
			segmentView.addSegment(searchCourseLink, false);
		}
		
		putInitialPanel(mainPanel);
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) {
			if(isGuestOnly) {
				doOpenMyCourses(ureq);
				segmentView.select(myCourseLink);
			} else {
				boolean markEmpty = doOpenMark(ureq).isEmpty();
				if(markEmpty) {
					doOpenMyCourses(ureq);
					segmentView.select(myCourseLink);
				} else {
					segmentView.select(favoriteLink);
				}
			}
		} else {
			ContextEntry entry = entries.get(0);
			String segment = entry.getOLATResourceable().getResourceableTypeName();
			List<ContextEntry> subEntries = entries.subList(1, entries.size());
			if("Favorits".equalsIgnoreCase(segment)) {
				if(isGuestOnly) {
					doOpenMyCourses(ureq).activate(ureq, subEntries, entry.getTransientState());
					segmentView.select(myCourseLink);
				} else {
					doOpenMark(ureq).activate(ureq, subEntries, entry.getTransientState());
					segmentView.select(favoriteLink);
				}
			} else if("My".equalsIgnoreCase(segment)) {
				doOpenMyCourses(ureq).activate(ureq, subEntries, entry.getTransientState());
				segmentView.select(myCourseLink);
			} else if(("Catalog".equalsIgnoreCase(segment) || "CatalogEntry".equalsIgnoreCase(segment))
					&& catalogLink != null) {
				CatalogNodeController ctrl = doOpenCatalog(ureq);
				if(ctrl != null) {
					ctrl.activate(ureq, entries, entry.getTransientState());
					segmentView.select(catalogLink);
				}
			} else if("Search".equalsIgnoreCase(segment) && searchCourseLink != null) {
				doOpenSearchCourses(ureq).activate(ureq, subEntries, entry.getTransientState());
				segmentView.select(searchCourseLink);
			} else {
				//default if the others fail
				doOpenMyCourses(ureq).activate(ureq, subEntries, entry.getTransientState());
				segmentView.select(myCourseLink);
			}
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				cleanUp();
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == favoriteLink) {
					doOpenMark(ureq);
				} else if (clickedLink == myCourseLink) {
					doOpenMyCourses(ureq);
				} else if (clickedLink == catalogLink) {
					doOpenCatalog(ureq);
				} else if(clickedLink == searchCourseLink) {
					doOpenSearchCourses(ureq);
				}
			}
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(catalogCtrl);
		removeAsListenerAndDispose(markedCtrl);
		removeAsListenerAndDispose(myCoursesCtrl);
		removeAsListenerAndDispose(searchCoursesCtrl);
		catalogCtrl = null;
		markedCtrl = null;
		myCoursesCtrl = null;
		searchCoursesCtrl = null;
	}
	
	private RepositoryEntryListController doOpenMark(UserRequest ureq) {
		cleanUp();
		
		SearchMyRepositoryEntryViewParams searchParams
			= new SearchMyRepositoryEntryViewParams(getIdentity(), ureq.getUserSession().getRoles());
		searchParams.setMarked(Boolean.TRUE);

		OLATResourceable ores = OresHelper.createOLATResourceableInstance("Favorits", 0l);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ores, null, getWindowControl());
		markedStackPanel = new BreadcrumbedStackedPanel("mrkstack", getTranslator(), this);
		markedCtrl = new RepositoryEntryListController(ureq, bwControl, searchParams, true, false, "marked", markedStackPanel);
		markedStackPanel.pushController(translate("search.mark"), markedCtrl);
		listenTo(markedCtrl);

		addToHistory(ureq, markedCtrl);
		mainVC.put("segmentCmp", markedStackPanel);
		return markedCtrl;
	}
	
	private RepositoryEntryListController doOpenMyCourses(UserRequest ureq) {
		cleanUp();
	
		SearchMyRepositoryEntryViewParams searchParams
			= new SearchMyRepositoryEntryViewParams(getIdentity(), ureq.getUserSession().getRoles());
		searchParams.setMembershipMandatory(true);

		OLATResourceable ores = OresHelper.createOLATResourceableInstance("My", 0l);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ores, null, getWindowControl());
		myCoursesStackPanel = new BreadcrumbedStackedPanel("mystack", getTranslator(), this);
		myCoursesCtrl = new RepositoryEntryListController(ureq, bwControl, searchParams, true, false, "my", myCoursesStackPanel);
		myCoursesStackPanel.pushController(translate("search.mycourses.student"), myCoursesCtrl);
		listenTo(myCoursesCtrl);

		addToHistory(ureq, myCoursesCtrl);
		mainVC.put("segmentCmp", myCoursesStackPanel);
		return myCoursesCtrl;
	}
	
	private CatalogNodeController doOpenCatalog(UserRequest ureq) {
		if(!repositoryModule.isCatalogEnabled() || !repositoryModule.isCatalogBrowsingEnabled()) {
			return null;
		}
		cleanUp();

		List<CatalogEntry> entries = catalogManager.getRootCatalogEntries();
		CatalogEntry rootEntry = null;
		if(entries.size() > 0) {
			rootEntry = entries.get(0);
		}
		
		OLATResourceable ores = OresHelper.createOLATResourceableInstance("Catalog", 0l);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ores, null, getWindowControl());
		catalogStackPanel = new BreadcrumbedStackedPanel("catstack", getTranslator(), this);
		catalogCtrl = new CatalogNodeController(ureq, bwControl, getWindowControl(), rootEntry, catalogStackPanel, false);
		catalogStackPanel.pushController(translate("search.catalog"), catalogCtrl);
		listenTo(catalogCtrl);

		addToHistory(ureq, catalogCtrl);
		mainVC.put("segmentCmp", catalogStackPanel);
		return catalogCtrl;
	}
	
	private RepositoryEntryListController doOpenSearchCourses(UserRequest ureq) {
		cleanUp();

		SearchMyRepositoryEntryViewParams searchParams
			= new SearchMyRepositoryEntryViewParams(getIdentity(), ureq.getUserSession().getRoles());
		searchParams.setMembershipMandatory(false);

		OLATResourceable ores = OresHelper.createOLATResourceableInstance("Search", 0l);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ores, null, getWindowControl());
		searchCoursesStackPanel = new BreadcrumbedStackedPanel("search", getTranslator(), this);
		searchCoursesCtrl = new RepositoryEntryListController(ureq, bwControl, searchParams, false, true, "my-search", searchCoursesStackPanel);
		searchCoursesStackPanel.pushController(translate("search.mycourses.student"), searchCoursesCtrl);
		listenTo(searchCoursesCtrl);
		
		addToHistory(ureq, searchCoursesCtrl);
		mainVC.put("segmentCmp", searchCoursesStackPanel);
		return searchCoursesCtrl;
	}
}
