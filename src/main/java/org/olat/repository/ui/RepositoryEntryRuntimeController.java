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
package org.olat.repository.ui;

import java.util.Collections;
import java.util.List;

import org.olat.NewControllerFactory;
import org.olat.core.commons.services.mark.Mark;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.dropdown.Dropdown.Spacer;
import org.olat.core.gui.components.htmlheader.jscss.CustomCSS;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.PopEvent;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.HistoryPoint;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.assessment.AssessmentModeManager;
import org.olat.course.assessment.model.TransientAssessmentMode;
import org.olat.course.run.RunMainController;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.EntryChangedEvent;
import org.olat.repository.controllers.EntryChangedEvent.Change;
import org.olat.repository.handlers.EditionSupport;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.repository.ui.author.AuthoringEditAccessController;
import org.olat.repository.ui.author.CatalogSettingsController;
import org.olat.repository.ui.author.CopyRepositoryEntryController;
import org.olat.repository.ui.author.RepositoryEditDescriptionController;
import org.olat.repository.ui.author.RepositoryMembersController;
import org.olat.repository.ui.author.wizard.CloseResourceCallback;
import org.olat.repository.ui.author.wizard.Close_1_ExplanationStep;
import org.olat.repository.ui.list.RepositoryEntryDetailsController;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.ui.AccessEvent;
import org.olat.resource.accesscontrol.ui.AccessListController;
import org.olat.resource.accesscontrol.ui.AccessRefusedController;
import org.olat.resource.accesscontrol.ui.OrdersAdminController;
import org.olat.user.UserManager;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 14.08.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class RepositoryEntryRuntimeController extends MainLayoutBasicController implements Activateable2 {

	private Controller runtimeController;
	protected final TooledStackedPanel toolbarPanel;
	private final RuntimeControllerCreator runtimeControllerCreator;
	
	protected Controller editorCtrl;
	protected Controller currentToolCtr;
	private CloseableModalController cmc;
	protected Controller accessController;
	private OrdersAdminController ordersCtlr;
	private StepsMainRunController closeCtrl;
	private DialogBoxController deleteDialogCtrl;
	private CatalogSettingsController catalogCtlr;
	private CopyRepositoryEntryController copyCtrl;
	protected AuthoringEditAccessController accessCtrl;
	private RepositoryEntryDetailsController detailsCtrl;
	private RepositoryMembersController membersEditController;
	private RepositoryEditDescriptionController descriptionCtrl;
	
	private Dropdown tools;
	private Dropdown settings;
	protected Link editLink, membersLink, ordersLink,
				 editDescriptionLink, accessLink, catalogLink,
				 detailsLink, bookmarkLink,
				 copyLink, downloadLink, closeLink, deleteLink;
	
	protected final boolean isOlatAdmin;
	protected final boolean isGuestOnly;
	protected final boolean isAuthor;
	
	protected RepositoryEntrySecurity reSecurity;
	protected final Roles roles;

	protected final boolean showInfos;
	protected final boolean allowBookmark;
	
	protected boolean corrupted;
	private RepositoryEntry re;
	private LockResult lockResult;
	private boolean assessmentLock;// by Assessment mode
	private AssessmentMode assessmentMode;
	private final RepositoryHandler handler;
	
	private HistoryPoint launchedFromPoint;
	
	@Autowired
	protected ACService acService;
	@Autowired
	protected UserManager userManager;
	@Autowired
	protected MarkManager markManager;
	@Autowired
	protected RepositoryModule repositoryModule;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	protected RepositoryManager repositoryManager;
	@Autowired
	private RepositoryHandlerFactory handlerFactory;
	@Autowired
	private AssessmentModeManager assessmentModeMgr;
	
	public RepositoryEntryRuntimeController(UserRequest ureq, WindowControl wControl, RepositoryEntry re,
			RepositoryEntrySecurity reSecurity, RuntimeControllerCreator runtimeControllerCreator) {
		this(ureq, wControl, re, reSecurity, runtimeControllerCreator, true, true);
	}

	public RepositoryEntryRuntimeController(UserRequest ureq, WindowControl wControl, RepositoryEntry re,
			RepositoryEntrySecurity reSecurity, RuntimeControllerCreator runtimeControllerCreator, boolean allowBookmark, boolean showInfos) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));
		
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable
				.wrapBusinessPath(OresHelper.createOLATResourceableType("RepositorySite")));
		
		//! check corrupted
		corrupted = isCorrupted(re);
		assessmentLock = isAssessmentLock(ureq, re, reSecurity);
		
		this.re = re;
		this.showInfos = showInfos;
		this.allowBookmark = allowBookmark;
		this.runtimeControllerCreator = runtimeControllerCreator;
		
		UserSession session = ureq.getUserSession();
		
		if(assessmentLock) {
			TransientAssessmentMode mode = ureq.getUserSession().getLockMode();
			this.assessmentMode = assessmentModeMgr.getAssessmentModeById(mode.getModeKey());
		}
		
		if(session != null &&  session.getHistoryStack() != null && session.getHistoryStack().size() >= 2) {
			// Set previous business path as back link for this course - brings user back to place from which he launched the course
			List<HistoryPoint> stack = session.getHistoryStack();
			for(int i=stack.size() - 2; i-->0; ) {
				HistoryPoint point = stack.get(stack.size() - 2);
				if(point.getEntries().size() > 0) {
					OLATResourceable ores = point.getEntries().get(0).getOLATResourceable();
					if(!OresHelper.equals(re, ores) && !OresHelper.equals(re.getOlatResource(), ores)) {
						launchedFromPoint = point;
						break;
					}
				}
			}
		}
		
		handler = handlerFactory.getRepositoryHandler(re);

		roles = ureq.getUserSession().getRoles();
		isOlatAdmin = roles.isOLATAdmin();
		isGuestOnly = roles.isGuestOnly();
		isAuthor = roles.isAuthor();
		this.reSecurity = reSecurity;

		// set up the components
		toolbarPanel = new TooledStackedPanel("courseStackPanel", getTranslator(), this);
		toolbarPanel.setInvisibleCrumb(0); // show root (course) level
		toolbarPanel.setShowCloseLink(!assessmentLock, !assessmentLock);
		toolbarPanel.getBackLink().setEnabled(!assessmentLock);
		putInitialPanel(toolbarPanel);
		doRun(ureq, reSecurity);
		loadRights(reSecurity);
		initToolbar();
	}
	
	protected boolean isCorrupted(RepositoryEntry entry) {
		return entry == null;
	}
	
	protected final boolean isAssessmentLock() {
		return assessmentLock;
	}
	
	private final boolean isAssessmentLock(UserRequest ureq, RepositoryEntry entry, RepositoryEntrySecurity reSec) {
		OLATResource resource = entry.getOlatResource();
		OLATResourceable lock = ureq.getUserSession().getLockResource();
		return lock != null && !reSec.isOwner() && !ureq.getUserSession().getRoles().isOLATAdmin()
				&& lock.getResourceableId().equals(resource.getResourceableId())
				&& lock.getResourceableTypeName().equals(resource.getResourceableTypeName());
	}
	
	/**
	 * If override, need to set isOwner and isEntryAdmin
	 */
	protected void loadRights(RepositoryEntrySecurity security) {
		this.reSecurity = security;
	}
	
	protected RepositoryEntry getRepositoryEntry() {
		return re;
	}
	
	protected void loadRepositoryEntry() {
		re = repositoryService.loadByKey(re.getKey());
	}
	
	protected OLATResourceable getOlatResourceable() {
		return OresHelper.clone(re.getOlatResource());
	}
	
	protected Controller getRuntimeController() {
		return runtimeController;
	}

	protected final void initToolbar() {
		tools = new Dropdown("toolbox.tools", "toolbox.tools", false, getTranslator());
		tools.setElementCssClass("o_sel_repository_tools");
		tools.setIconCSS("o_icon o_icon_tools");
		
		String resourceType = re.getOlatResource().getResourceableTypeName();
		String name = NewControllerFactory.translateResourceableTypeName(resourceType, getLocale());
		settings = new Dropdown("settings", "toolbox.settings", false, getTranslator());
		settings.setTranslatedLabel(name);
		settings.setElementCssClass("o_sel_course_settings");
		settings.setIconCSS("o_icon o_icon_actions");
		
		initToolbar(tools, settings);
		
		if(tools.size() > 0) {
			toolbarPanel.addTool(tools, Align.left, true);
		}
		if(settings.size() > 0) {
			toolbarPanel.addTool(settings, Align.left, true);
		}
	}
	
	protected void initToolbar(Dropdown toolsDropdown, Dropdown settingsDropdown) {
		if (reSecurity.isEntryAdmin()) {
			//tools
			if(handler.supportsEdit(re.getOlatResource()) == EditionSupport.yes) {
				boolean managed = RepositoryEntryManagedFlag.isManaged(getRepositoryEntry(), RepositoryEntryManagedFlag.editcontent);
				editLink = LinkFactory.createToolLink("edit.cmd", translate("details.openeditor"), this, "o_sel_repository_editor");
				editLink.setIconLeftCSS("o_icon o_icon-lg o_icon_edit");
				editLink.setEnabled(!managed);
				toolsDropdown.addComponent(editLink);
			}
			
			membersLink = LinkFactory.createToolLink("members", translate("details.members"), this, "o_sel_repo_members");
			membersLink.setIconLeftCSS("o_icon o_icon-fw o_icon_membersmanagement");
			toolsDropdown.addComponent(membersLink);
			
			ordersLink = LinkFactory.createToolLink("bookings", translate("details.orders"), this, "o_sel_repo_booking");
			ordersLink.setIconLeftCSS("o_icon o_icon-fw o_icon_booking");
			boolean booking = acService.isResourceAccessControled(re.getOlatResource(), null);
			ordersLink.setEnabled(booking);
			toolsDropdown.addComponent(ordersLink);	
		}
		
		initSettingsTools(settingsDropdown);
		initEditionTools(settingsDropdown);

		detailsLink = LinkFactory.createToolLink("details", translate("details.header"), this, "o_sel_repo_details");
		detailsLink.setIconLeftCSS("o_icon o_icon-fw o_icon_details");
		detailsLink.setElementCssClass("o_sel_author_details");
		detailsLink.setVisible(showInfos);
		toolbarPanel.addTool(detailsLink);
		
		boolean marked = markManager.isMarked(re, getIdentity(), null);
		String css = marked ? Mark.MARK_CSS_ICON : Mark.MARK_ADD_CSS_ICON;
		bookmarkLink = LinkFactory.createToolLink("bookmark", translate("details.bookmark.label"), this, css);
		bookmarkLink.setTitle(translate(marked ? "details.bookmark.remove" : "details.bookmark"));
		bookmarkLink.setVisible(allowBookmark);
		toolbarPanel.addTool(bookmarkLink, Align.right);
	}
	
	protected void initSettingsTools(Dropdown settingsDropdown) {
		if (reSecurity.isEntryAdmin()) {
			//settings
			editDescriptionLink = LinkFactory.createToolLink("settings.cmd", translate("details.chprop"), this, "o_icon_details");
			editDescriptionLink.setElementCssClass("o_sel_course_settings");
			editDescriptionLink.setEnabled(!corrupted);
			settingsDropdown.addComponent(editDescriptionLink);
			
			accessLink = LinkFactory.createToolLink("access.cmd", translate("tab.accesscontrol"), this, "o_icon_password");
			accessLink.setElementCssClass("o_sel_course_access");
			settingsDropdown.addComponent(accessLink);
			
			catalogLink = LinkFactory.createToolLink("cat", translate("details.categoriesheader"), this, "o_icon_catalog");
			catalogLink.setElementCssClass("o_sel_repo_add_to_catalog");
			catalogLink.setVisible(repositoryModule.isCatalogEnabled());
			settingsDropdown.addComponent(catalogLink);
		}
	}
	
	protected void initEditionTools(Dropdown settingsDropdown) {
		boolean copyManaged = RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.copy);
		boolean canCopy = (isAuthor || reSecurity.isEntryAdmin()) && (re.getCanCopy() || reSecurity.isEntryAdmin()) && !copyManaged;
		
		boolean canDownload = re.getCanDownload() && handler.supportsDownload();
		// disable download for courses if not author or owner
		if (re.getOlatResource().getResourceableTypeName().equals(CourseModule.getCourseTypeName()) && !(reSecurity.isEntryAdmin() || isAuthor)) {
			canDownload = false;
		}
		// always enable download for owners
		if (reSecurity.isEntryAdmin() && handler.supportsDownload()) {
			canDownload = true;
		}
		
		if(canCopy || canDownload) {
			if(settingsDropdown.size() > 0) {
				settingsDropdown.addComponent(new Spacer("copy-download"));
			}
			if (canCopy) {
				copyLink = LinkFactory.createToolLink("copy", translate("details.copy"), this, "o_icon o_icon-fw o_icon_copy");
				copyLink.setElementCssClass("o_sel_repo_copy");
				settingsDropdown.addComponent(copyLink);
			}
			if(canDownload) {
				downloadLink = LinkFactory.createToolLink("download", translate("details.download"), this, "o_icon o_icon-fw o_icon_download");
				downloadLink.setElementCssClass("o_sel_repo_download");
				settingsDropdown.addComponent(downloadLink);
			}
		}
		
		boolean canClose = OresHelper.isOfType(re.getOlatResource(), CourseModule.class)
				&& !RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.close)
				&& !RepositoryManager.getInstance().createRepositoryEntryStatus(re.getStatusCode()).isClosed();
		
		if(reSecurity.isEntryAdmin()) {
			boolean deleteManaged = RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.delete);
			if(settingsDropdown.size() > 0 && (canClose || !deleteManaged)) {
				settingsDropdown.addComponent(new Spacer("close-delete"));
			}

			if(canClose) {
				closeLink = LinkFactory.createToolLink("close", translate("details.close.ressoure"), this, "o_icon o_icon-fw o_icon_close_resource");
				closeLink.setElementCssClass("o_sel_repo_close");
				settingsDropdown.addComponent(closeLink);
			}
			if(!deleteManaged) {
				deleteLink = LinkFactory.createToolLink("delete", translate("details.delete"), this, "o_icon o_icon-fw o_icon_delete_item");
				deleteLink.setElementCssClass("o_sel_repo_close");
				settingsDropdown.addComponent(deleteLink);
			}
		}
	}
	
	public void setActiveTool(Link tool) {
		if(tools != null) {
			tools.setActiveLink(tool);
		}
		if(settings != null) {
			settings.setActiveLink(tool);
		}
	}
	
	@Override
	public CustomCSS getCustomCSS() {
		return runtimeController instanceof MainLayoutController ? ((MainLayoutController)runtimeController).getCustomCSS() : null;
	}

	@Override
	public void setCustomCSS(CustomCSS newCustomCSS) {
		if(runtimeController instanceof MainLayoutController) {
			((MainLayoutController)runtimeController).setCustomCSS(newCustomCSS);
		}
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		entries = removeRepositoryEntry(entries);
		if(entries != null && entries.size() > 0) {
			String type = entries.get(0).getOLATResourceable().getResourceableTypeName();
			if("Editor".equalsIgnoreCase(type)) {
				if(handler.supportsEdit(re) == EditionSupport.yes
						&& !repositoryManager.createRepositoryEntryStatus(re.getStatusCode()).isClosed()) {
					doEdit(ureq);
				}
			} else if("Catalog".equalsIgnoreCase(type)) {
				doCatalog(ureq);
			} else if("Infos".equalsIgnoreCase(type)) {
				doDetails(ureq);	
			} else if("EditDescription".equalsIgnoreCase(type)) {
				doEditSettings(ureq);
			} else if("MembersMgmt".equalsIgnoreCase(type)) {
				doMembers(ureq);
			}
		}

		if(runtimeController instanceof Activateable2) {
			((Activateable2)runtimeController).activate(ureq, entries, state);
		}
	}
	
	protected List<ContextEntry> removeRepositoryEntry(List<ContextEntry> entries) {
		if(entries != null && entries.size() > 0) {
			String type = entries.get(0).getOLATResourceable().getResourceableTypeName();
			if("RepositoryEntry".equals(type)) {
				if(entries.size() > 1) {
					entries = entries.subList(1, entries.size());
				} else {
					entries = Collections.emptyList();
				}
			}
		}
		return entries;
	}

	@Override
	protected void doDispose() {
		if(runtimeController != null && !runtimeController.isDisposed()) {
			runtimeController.dispose();
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == runtimeController) {
			fireEvent(ureq, event);
		} else if(editLink == source) {
			doEdit(ureq);
		} else if(membersLink == source) {
			doMembers(ureq);
		} else if(editDescriptionLink == source) {
			doEditSettings(ureq);
		} else if(accessLink == source) {
			doAccess(ureq);
		} else if(catalogLink == source) {
			doCatalog(ureq);
		} else if(ordersLink == source) {
			doOrders(ureq);
		} else if(detailsLink == source) {
			doDetails(ureq);
		} else if(bookmarkLink == source) {
			boolean marked = doMark();
			String css = "o_icon " + (marked ? Mark.MARK_CSS_ICON : Mark.MARK_ADD_CSS_ICON);
			bookmarkLink.setIconLeftCSS(css);
			bookmarkLink.setTitle( translate(marked ? "details.bookmark.remove" : "details.bookmark"));
		} else if(copyLink == source) {
			doCopy(ureq);
		} else if(downloadLink == source) {
			doDownload(ureq);
		} else if(closeLink == source) {
			doCloseResourceWizard(ureq);
		} else if(deleteLink == source) {
			doDelete(ureq);
		} else if(source == toolbarPanel) {
			if (event == Event.CLOSE_EVENT) {
				doClose(ureq);
			} else if(event instanceof PopEvent) {
				setActiveTool(null);
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(cmc == source) {
			cleanUp();
		} else if (source == accessController) {
			if(event.equals(AccessEvent.ACCESS_OK_EVENT)) {
				reSecurity = repositoryManager.isAllowed(ureq, getRepositoryEntry());
				launchContent(ureq, reSecurity);
				cleanUp();
			} else if(event.equals(AccessEvent.ACCESS_FAILED_EVENT)) {
				String msg = ((AccessEvent)event).getMessage();
				if(StringHelper.containsNonWhitespace(msg)) {
					getWindowControl().setError(msg);
				} else {
					showError("error.accesscontrol");
				}
			}
		} else if(accessCtrl == source) {
			if(event == Event.CHANGED_EVENT) {
				re = accessCtrl.getEntry();
			}
		} else if(descriptionCtrl == source) {
			if(event == Event.CHANGED_EVENT) {
				re = descriptionCtrl.getEntry();
			}
		} else if(closeCtrl == source) {
			if(event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				getWindowControl().pop();
				removeAsListenerAndDispose(closeCtrl);
				closeCtrl = null;
				if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
					doCloseResource(ureq);
				}
			}
		} else if(deleteDialogCtrl == source) {
			if (DialogBoxUIFactory.isYesEvent(event)) {
				doCompleteDelete(ureq);
			}
		} else if(copyCtrl == source) {
			cmc.deactivate();
			if (event == Event.DONE_EVENT) {
				RepositoryEntryRef copy = copyCtrl.getCopiedEntry();
				String businessPath = "[RepositoryEntry:" + copy.getKey() + "][EditDescription:0]";
				NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
			}
			cleanUp();
		}
	}
	
	protected RepositoryEntryRuntimeController popToRoot(UserRequest ureq) {
		toolbarPanel.popUpToRootController(ureq);
		return this;
	}
	
	protected void cleanUp() {
		removeAsListenerAndDispose(membersEditController);
		removeAsListenerAndDispose(accessController);
		removeAsListenerAndDispose(descriptionCtrl);
		removeAsListenerAndDispose(catalogCtlr);
		removeAsListenerAndDispose(detailsCtrl);
		removeAsListenerAndDispose(editorCtrl);
		removeAsListenerAndDispose(ordersCtlr);
		removeAsListenerAndDispose(closeCtrl);
		removeAsListenerAndDispose(copyCtrl);
		removeAsListenerAndDispose(cmc);
		
		membersEditController = null;
		accessController = null;
		descriptionCtrl = null;
		catalogCtlr = null;
		detailsCtrl = null;
		editorCtrl = null;
		ordersCtlr = null;
		closeCtrl = null;
		copyCtrl = null;
		cmc = null;
	}
	
	/**
	 * Pop to root, clean up, and push
	 * @param ureq
	 * @param name
	 * @param controller
	 */
	protected <T extends Controller> T pushController(UserRequest ureq, String name, T controller) {
		popToRoot(ureq).cleanUp();
		toolbarPanel.pushController(name, controller);
		if(controller instanceof ToolbarAware) {
			((ToolbarAware)controller).initToolbar();
		}
		return controller;
	}
	
	/**
	 * Open the editor for all repository entry metadata, access control...
	 * @param ureq
	 */
	protected void doAccess(UserRequest ureq) {
		AuthoringEditAccessController ctrl = new AuthoringEditAccessController(ureq, getWindowControl(), re);
		listenTo(ctrl);
		accessCtrl = pushController(ureq, translate("tab.accesscontrol"), ctrl);
		setActiveTool(accessLink);
		currentToolCtr = accessCtrl;
	}
	
	protected final void doClose(UserRequest ureq) {
		// Now try to go back to place that is attacked to (optional) root back business path
		if (launchedFromPoint != null && StringHelper.containsNonWhitespace(launchedFromPoint.getBusinessPath())) {
			BusinessControl bc = BusinessControlFactory.getInstance().createFromPoint(launchedFromPoint);
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
			try {
				//make the resume secure. If something fail, don't generate a red screen
				NewControllerFactory.getInstance().launch(ureq, bwControl);
			} catch (Exception e) {
				logError("Error while resuming with root leve back business path::" + launchedFromPoint.getBusinessPath(), e);
			}
		}
		
		// Navigate beyond the stack, our own layout has been popped - close this tab
		DTabs tabs = getWindowControl().getWindowBackOffice().getWindow().getDTabs();
		if (tabs != null) {
			DTab tab = tabs.getDTab(re.getOlatResource());
			if (tab != null) {
				tabs.removeDTab(ureq, tab);						
			}
		}
	}
	
	protected void doEdit(UserRequest ureq) {
		if(!reSecurity.isEntryAdmin()) return;
		
		Controller ctrl = handler.createEditorController(re, ureq, getWindowControl(), toolbarPanel);
		if(ctrl != null) {
			listenTo(ctrl);
			editorCtrl = pushController(ureq, translate("resource.editor"), ctrl);
			currentToolCtr = editorCtrl;
			setActiveTool(editLink);
		}
	}
	
	protected void doDetails(UserRequest ureq) {
		RepositoryEntryDetailsController ctrl = new RepositoryEntryDetailsController(ureq, getWindowControl(), re);
		listenTo(ctrl);
		detailsCtrl = pushController(ureq, translate("details.header"), ctrl);
		currentToolCtr = detailsCtrl;
	}
	
	/**
	 * Open the editor for all repository entry metadata, access control...
	 * @param ureq
	 */
	protected void doEditSettings(UserRequest ureq) {
		if(!reSecurity.isEntryAdmin()) return;
		
		RepositoryEditDescriptionController ctrl = new RepositoryEditDescriptionController(ureq, getWindowControl(), re, false);
		listenTo(ctrl);
		descriptionCtrl = pushController(ureq, translate("settings.editor"), ctrl);
		currentToolCtr = descriptionCtrl;
		setActiveTool(editDescriptionLink);
	}
	
	/**
	 * Internal helper to initiate the add to catalog workflow
	 * @param ureq
	 */
	protected void doCatalog(UserRequest ureq) {
		if(!reSecurity.isEntryAdmin()) return;
		
		popToRoot(ureq).cleanUp();
		catalogCtlr = new CatalogSettingsController(ureq, getWindowControl(), toolbarPanel, re);
		listenTo(catalogCtlr);
		catalogCtlr.initToolbar();
		currentToolCtr = catalogCtlr;
		setActiveTool(catalogLink);
	}
	
	protected Activateable2 doMembers(UserRequest ureq) {
		if(!reSecurity.isEntryAdmin()) return null;

		RepositoryMembersController ctrl = new RepositoryMembersController(ureq, getWindowControl(), re);
		listenTo(ctrl);
		membersEditController = pushController(ureq, translate("details.members"), ctrl);
		currentToolCtr = membersEditController;
		setActiveTool(membersLink);
		return membersEditController;
	}
	
	protected void doOrders(UserRequest ureq) {
		if(!reSecurity.isEntryAdmin()) return;

		OrdersAdminController ctrl = new OrdersAdminController(ureq, getWindowControl(), re.getOlatResource());
		listenTo(ctrl);
		ordersCtlr = pushController(ureq, translate("details.orders"), ctrl);
		currentToolCtr = ordersCtlr;
		setActiveTool(ordersLink);
	}
	
	private void doRun(UserRequest ureq, RepositoryEntrySecurity security) {
		if(ureq.getUserSession().getRoles().isOLATAdmin()) {
			launchContent(ureq, security);
		} else {
			// guest are allowed to see resource with BARG 
			if(re.getAccess() == RepositoryEntry.ACC_USERS_GUESTS && ureq.getUserSession().getRoles().isGuestOnly()) {
				launchContent(ureq, security);
			} else {
				AccessResult acResult = acService.isAccessible(re, getIdentity(), security.isMember(), false);
				if(acResult.isAccessible()) {
					launchContent(ureq, security);
				} else if (re != null && acResult.getAvailableMethods().size() > 0) {
					accessController = new AccessListController(ureq, getWindowControl(), acResult.getAvailableMethods());
					listenTo(accessController);
					toolbarPanel.rootController(re.getDisplayname(), accessController);
				} else {
					Controller ctrl = new AccessRefusedController(ureq, getWindowControl());
					listenTo(ctrl);
					toolbarPanel.rootController(re.getDisplayname(), ctrl);
				}
			}
		}
	}
	
	protected boolean doMark() {
		OLATResourceable item = OresHelper.clone(re);
		if(markManager.isMarked(item, getIdentity(), null)) {
			markManager.removeMark(item, getIdentity(), null);
			return false;
		} else {
			String businessPath = "[RepositoryEntry:" + item.getResourceableId() + "]";
			markManager.setMark(item, getIdentity(), null, businessPath);
			return true;
		}
	}
	
	private void doCopy(UserRequest ureq) {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(copyCtrl);

		copyCtrl = new CopyRepositoryEntryController(ureq, getWindowControl(), re);
		listenTo(copyCtrl);
		
		String title = translate("details.copy");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), copyCtrl.getInitialComponent(), true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doDownload(UserRequest ureq) {
		if (handler == null) {
			StringBuilder sb = new StringBuilder(translate("error.download"));
			sb.append(": No download handler for repository entry: ").append(
					re.getKey());
			showError(sb.toString());
			return;
		}

		RepositoryEntry entry = repositoryService.loadByKey(re.getKey());
		OLATResourceable ores = entry.getOlatResource();
		if (ores == null) {
			showError("error.download");
			return;
		}

		boolean isAlreadyLocked = handler.isLocked(ores);
		try {
			lockResult = handler.acquireLock(ores, ureq.getIdentity());
			if (lockResult == null
					|| (lockResult != null && lockResult.isSuccess() && !isAlreadyLocked)) {
				MediaResource mr = handler.getAsMediaResource(ores, false);
				if (mr != null) {
					repositoryService.incrementDownloadCounter(entry);
					ureq.getDispatchResult().setResultingMediaResource(mr);
				} else {
					showError("error.export");
					fireEvent(ureq, Event.FAILED_EVENT);
				}
			} else if (lockResult != null && lockResult.isSuccess()
					&& isAlreadyLocked) {
				String fullName = userManager.getUserDisplayName(lockResult
						.getOwner());
				showInfo("warning.course.alreadylocked.bySameUser", fullName);
				lockResult = null; // invalid lock, it was already locked
			} else {
				String fullName = userManager.getUserDisplayName(lockResult
						.getOwner());
				showInfo("warning.course.alreadylocked", fullName);
			}
		} finally {
			if ((lockResult != null && lockResult.isSuccess() && !isAlreadyLocked)) {
				handler.releaseLock(lockResult);
				lockResult = null;
			}
		}
	}
	
	private void doCloseResourceWizard(UserRequest ureq) {
		removeAsListenerAndDispose(closeCtrl);

		Step start = new Close_1_ExplanationStep(ureq, re);
		StepRunnerCallback finish = new CloseResourceCallback(re);
		closeCtrl = new StepsMainRunController(ureq, getWindowControl(), start, finish, null,
				translate("wizard.closecourse.title"), "o_sel_checklist_wizard");
		listenTo(closeCtrl);
		getWindowControl().pushAsModalDialog(closeCtrl.getInitialComponent());
	}
	
	/**
	 * Remove close and edit tools, if in edit mode, pop-up-to root
	 * @param ureq
	 */
	private void doCloseResource(UserRequest ureq) {
		loadRepositoryEntry();
		closeLink.setVisible(false);
		if(editLink != null) {
			editLink.setVisible(false);
		}
		if(currentToolCtr == editorCtrl) {
			toolbarPanel.popUpToRootController(ureq);
		}
	}
	
	private void doDelete(UserRequest ureq) {
		boolean isOwner = true;
		if (!isOwner) throw new OLATSecurityException("Trying to delete, but not allowed: user = " + ureq.getIdentity());

		//show how many users are currently using this resource
		String dialogTitle = translate("del.header", re.getDisplayname());
		Long resId = re.getResourceableId();
		OLATResourceable courseRunOres = OresHelper.createOLATResourceableInstance(RunMainController.ORES_TYPE_COURSE_RUN, resId);
		int cnt = CoordinatorManager.getInstance().getCoordinator().getEventBus().getListeningIdentityCntFor(courseRunOres);

		String dialogText = translate(corrupted ? "del.confirm.corrupted" : "del.confirm", String.valueOf(cnt));
		deleteDialogCtrl = activateYesNoDialog(ureq, dialogTitle, dialogText, deleteDialogCtrl);
		deleteDialogCtrl.setUserObject(re);
	}
	
	private void doCompleteDelete(UserRequest ureq) {
		ErrorList errors = repositoryService.delete(re, getIdentity(), roles, getLocale());
		if (errors.hasErrors()) {
			showInfo("info.could.not.delete.entry");
		} else {
			fireEvent(ureq, new EntryChangedEvent(re, getIdentity(), Change.deleted));
			showInfo("info.entry.deleted");
		}
		doClose(ureq);
	}
	
	protected void launchContent(UserRequest ureq, RepositoryEntrySecurity security) {
		if(security.canLaunch()) {
			runtimeController = runtimeControllerCreator.create(ureq, getWindowControl(), toolbarPanel, re, reSecurity, assessmentMode);
			listenTo(runtimeController);
			toolbarPanel.rootController(re.getDisplayname(), runtimeController);
		} else {
			runtimeController = new AccessRefusedController(ureq, getWindowControl());
			listenTo(runtimeController);
			toolbarPanel.rootController(re.getDisplayname(), runtimeController);
		}
	}

	public interface RuntimeControllerCreator {
		
		public Controller create(UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbarPanel,
				RepositoryEntry entry, RepositoryEntrySecurity reSecurity, AssessmentMode mode);
		
	}
	
	public interface ToolbarAware {
		
		public void initToolbar();
		
	}
}