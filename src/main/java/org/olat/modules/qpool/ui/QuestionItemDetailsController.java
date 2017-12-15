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
package org.olat.modules.qpool.ui;

import java.util.Collections;
import java.util.List;

import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingDefaultSecurityCallback;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingSecurityCallback;
import org.olat.core.commons.services.commentAndRating.ui.UserCommentsAndRatingsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.dropdown.DropdownOrientation;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.PopEvent;
import org.olat.core.gui.components.stack.TooledController;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.model.BusinessGroupSelectionEvent;
import org.olat.group.ui.main.SelectBusinessGroupController;
import org.olat.modules.qpool.QPoolSPI;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemSecurityCallback;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.QuestionItemView;
import org.olat.modules.qpool.QuestionPoolModule;
import org.olat.modules.qpool.QuestionStatus;
import org.olat.modules.qpool.ReviewService;
import org.olat.modules.qpool.manager.ExportQItemResource;
import org.olat.modules.qpool.model.QuestionItemImpl;
import org.olat.modules.qpool.ui.events.QItemEdited;
import org.olat.modules.qpool.ui.events.QItemEvent;
import org.olat.modules.qpool.ui.events.QPoolEvent;
import org.olat.modules.qpool.ui.metadata.MetadatasController;
import org.olat.modules.taxonomy.TaxonomyLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 24.01.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QuestionItemDetailsController extends BasicController implements TooledController, Activateable2 {
	
	private Link editItem;
	private Link statusDraftLink;
	private Link statusReviewLink;
	private Link statusFinalLink;
	private Link statusEndOfLifeLink;
	private Link statusRevisedLink;
	private Link startReviewLink;
	private Link reviewLink;
	private Link deleteLink;
	private Link nextItemLink;
	private Link numberItemsLink;
	private Link previousItemLink;
	private Link showMetadataLink;
	private Link hideMetadataLink;
	private Link shareItemLink;
	private Link exportItemLink;
	private Link copyItemLink;
	private Dropdown statusDropdown;
	private Dropdown commandDropdown;

	private Controller editCtrl;
	private Controller previewCtrl;
	private CloseableModalController cmc;
	private final VelocityContainer mainVC;
	private ReviewStartController reviewStartCtrl;	
	private ReviewController reviewCtrl;
	private DialogBoxController confirmEndOfLifeCtrl;
	private DialogBoxController confirmDeleteBox;
	private LayoutMain3ColsController editMainCtrl;
	private SelectBusinessGroupController selectGroupCtrl;
	private final MetadatasController metadatasCtrl;
	private final UserCommentsAndRatingsController commentsAndRatingCtr;
	private final TooledStackedPanel stackPanel;

	private final QuestionItemsSource itemSource;
	private final QuestionItemSecurityCallback securityCallback;
	private final Integer itemIndex;
	private final int numberOfItems;
	
	@Autowired
	private QuestionPoolModule poolModule;
	@Autowired
	private QPoolService qpoolService;
	@Autowired
	private ReviewService reviewService;
	
	public QuestionItemDetailsController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			QuestionItem item, QuestionItemSecurityCallback securityCallback, QuestionItemsSource itemSource,
			Integer itemIndex, int numberOfItems) {
		super(ureq, wControl);
		this.stackPanel = stackPanel;
		stackPanel.addListener(this);
		this.securityCallback = securityCallback;
		this.itemIndex = itemIndex;
		this.numberOfItems = numberOfItems;
		this.itemSource = itemSource;
		
		metadatasCtrl = new MetadatasController(ureq, wControl, item, securityCallback);
		listenTo(metadatasCtrl);
		
		Roles roles = ureq.getUserSession().getRoles();
		boolean moderator = roles.isOLATAdmin();
		boolean anonymous = roles.isGuestOnly() || roles.isInvitee();
		CommentAndRatingSecurityCallback commentAndRatingSecurityCallback = new CommentAndRatingDefaultSecurityCallback(getIdentity(), moderator, anonymous);
		commentsAndRatingCtr = new UserCommentsAndRatingsController(ureq, getWindowControl(), item, null,
				commentAndRatingSecurityCallback, true, securityCallback.canRate(), true);
		listenTo(commentsAndRatingCtr);

		mainVC = createVelocityContainer("item_details");

		QPoolSPI spi = poolModule.getQuestionPoolProvider(item.getFormat());
		boolean canEditContent = securityCallback.canEditQuestion() && (spi != null && spi.isTypeEditable());
		if(canEditContent) {
			editItem = LinkFactory.createButton("edit", mainVC, this);
			editItem.setIconLeftCSS("o_icon o_icon_edit");
		}
		
		setPreviewController(ureq, item);
		mainVC.put("metadatas", metadatasCtrl.getInitialComponent());
		mainVC.put("comments", commentsAndRatingCtr.getInitialComponent());
		putInitialPanel(mainVC);
	}
	
	@Override
	public void initTools() {
		stackPanel.removeAllTools();
		
		commandDropdown = new Dropdown("commands", "commands", false, getTranslator());
		commandDropdown.setIconCSS("o_icon o_icon-fw o_icon_qitem_commands");
		commandDropdown.setOrientation(DropdownOrientation.normal);
		
		copyItemLink = LinkFactory.createToolLink("copy", translate("copy"), this);
		copyItemLink.setIconLeftCSS("o_icon o_icon-lg o_icon_qitem_copy");
		commandDropdown.addComponent(copyItemLink);
		
		if (securityCallback.canDelete()) {
			deleteLink = LinkFactory.createToolLink("delete.item", translate("delete.item"), this);
			deleteLink.setIconLeftCSS("o_icon o_icon-lg o_icon_qitem_delete");
			commandDropdown.addComponent(deleteLink);
		}
		
		exportItemLink = LinkFactory.createToolLink("export.item", translate("export.item"), this);
		exportItemLink.setIconLeftCSS("o_icon o_icon-lg o_icon_qitem_export");
		commandDropdown.addComponent(exportItemLink);

		shareItemLink = LinkFactory.createToolLink("share.item", translate("share.item"), this);
		shareItemLink.setIconLeftCSS("o_icon o_icon-lg o_icon_qitem_share");
		commandDropdown.addComponent(shareItemLink);

		statusDropdown = new Dropdown("process.states", "process.states", false, getTranslator());
		statusDropdown.setIconCSS("o_icon o_icon-fw o_icon_" + metadatasCtrl.getItem().getQuestionStatus());
		statusDropdown.setOrientation(DropdownOrientation.normal);
		stackPanel.addTool(commandDropdown, Align.left);
		
		boolean hasDropdownComponents = false;
		if (securityCallback.canSetDraft()) {
			statusDraftLink = LinkFactory.createToolLink("lifecycle.status.draft", translate("lifecycle.status.draft"), this);
			statusDraftLink.setIconLeftCSS("o_icon o_icon-lg o_icon_draft o_qpool_draft");
			statusDropdown.addComponent(statusDraftLink);
			hasDropdownComponents = true;
		}
		if (securityCallback.canSetRevised()) {
			statusRevisedLink = LinkFactory.createToolLink("lifecycle.status.revised", translate("lifecycle.status.revised"), this);
			statusRevisedLink.setIconLeftCSS("o_icon o_icon-lg o_icon_revised o_qpool_revised");
			statusDropdown.addComponent(statusRevisedLink);
			hasDropdownComponents = true;
		}
		if (securityCallback.canSetReview()) {
			statusReviewLink = LinkFactory.createToolLink("lifecycle.status.review", translate("lifecycle.status.review"), this);
			statusReviewLink.setIconLeftCSS("o_icon o_icon-lg o_icon_review o_qpool_review");
			statusDropdown.addComponent(statusReviewLink);
			hasDropdownComponents = true;
		}
		if (securityCallback.canSetFinal()) {
			statusFinalLink = LinkFactory.createToolLink("lifecycle.status.finalVersion", translate("lifecycle.status.finalVersion"), this);
			statusFinalLink.setIconLeftCSS("o_icon o_icon-lg o_icon_finalVersion o_qpool_final");
			statusDropdown.addComponent(statusFinalLink);
			hasDropdownComponents = true;
		}
		if (securityCallback.canSetEndOfLife()) {
			statusEndOfLifeLink = LinkFactory.createToolLink("lifecycle.status.endOfLife", translate("lifecycle.status.endOfLife"), this);
			statusEndOfLifeLink.setIconLeftCSS("o_icon o_icon-lg o_icon_endOfLife o_qpool_end_of_life");
			statusDropdown.addComponent(statusEndOfLifeLink);
			hasDropdownComponents = true;
		}
		if (hasDropdownComponents) {
			stackPanel.addTool(statusDropdown, Align.left);
		}
		
		if (securityCallback.canStartReview()) {
			startReviewLink = LinkFactory.createToolLink("process.start.review", translate("process.start.review"), this);
			startReviewLink.setIconLeftCSS("o_icon o_icon-lg o_icon_review");
			stackPanel.addTool(startReviewLink, Align.left);
		}
		if (securityCallback.canReview() && reviewService.hasRatingController()) {
			reviewLink = LinkFactory.createToolLink("process.review", translate("process.review"), this);
			reviewLink.setIconLeftCSS("o_icon o_icon-lg o_icon_do_review");
			stackPanel.addTool(reviewLink, Align.left);
		}
		
		previousItemLink = LinkFactory.createToolLink("previous", translate("previous"), this);
		previousItemLink.setIconLeftCSS("o_icon o_icon-lg o_icon_previous");
		if (numberOfItems <= 1) {
			previousItemLink.setEnabled(false);
		}
		stackPanel.addTool(previousItemLink);
		
		String numbersOf = translate("item.numbers.of", new String[]{
				itemIndex != null? Integer.toString(itemIndex + 1): "",
				Integer.toString(numberOfItems) });
		numberItemsLink = LinkFactory.createToolLink("item.numbers.of", numbersOf, this);
		stackPanel.addTool(numberItemsLink);
		
		nextItemLink = LinkFactory.createToolLink("next", translate("next"), this);
		nextItemLink.setIconLeftCSS("o_icon io_icon-lg o_icon_next");
		if (numberOfItems <= 1) {
			nextItemLink.setEnabled(false);
		}
		stackPanel.addTool(nextItemLink);
		
		showMetadataLink = LinkFactory.createToolLink("metadata.show", translate("metadata.show"), this);
		showMetadataLink.setIconLeftCSS("o_icon o_icon-lg o_icon_edit_metadata");
		hideMetadataLink = LinkFactory.createToolLink("metadata.hide", translate("metadata.hide"), this);
		hideMetadataLink.setIconLeftCSS("o_icon o_icon-lg o_icon_edit_metadata");
		doHideMetadata();
	}
	
	protected void setPreviewController(UserRequest ureq, QuestionItem item) {
		QPoolSPI spi = poolModule.getQuestionPoolProvider(item.getFormat());
		if(spi == null) {
			previewCtrl = new QuestionItemRawController(ureq, getWindowControl());
		} else {
			previewCtrl = spi.getPreviewController(ureq, getWindowControl(), item, false);
			if(previewCtrl == null) {
				previewCtrl = new QuestionItemRawController(ureq, getWindowControl());
			}
		}
		listenTo(previewCtrl);
		if(mainVC != null) {
			mainVC.put("type_specifics", previewCtrl.getInitialComponent());
		}
	}
	
	@Override
	protected void doDispose() {
		if(stackPanel != null) {
			stackPanel.removeListener(this);
		}
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		String resourceTypeName = entries.get(0).getOLATResourceable().getResourceableTypeName();
		if("edit".equalsIgnoreCase(resourceTypeName)) {
			if(securityCallback.canEditQuestion() || metadatasCtrl.getItem() != null) {
				doEdit(ureq, metadatasCtrl.getItem());
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == statusDraftLink) {
			doStatusDraft(ureq, metadatasCtrl.getItem());
		} else if (source == statusRevisedLink) {
			doStatusRevised(ureq, metadatasCtrl.getItem());
		} else if (source == statusReviewLink) {
			doStatusReview(ureq, metadatasCtrl.getItem());
		} else if (source == statusFinalLink) {
			doStatusFinal(ureq, metadatasCtrl.getItem());
		} else if (source == statusEndOfLifeLink) {
			doConfirmEndOfLife(ureq, metadatasCtrl.getItem());
		} else if(source == startReviewLink) {
			doConfirmStartReview(ureq, metadatasCtrl.getItem());
		} else if (source == reviewLink) {
			openReview(ureq);
		} else if(source == deleteLink) {
			doConfirmDelete(ureq, metadatasCtrl.getItem());
		} else if(source == shareItemLink) {
			doSelectGroup(ureq, metadatasCtrl.getItem());
		} else if(source == exportItemLink) {
			doExport(ureq, metadatasCtrl.getItem());
		} else if(source == editItem) {
			doEdit(ureq, metadatasCtrl.getItem());
		} else if(source == copyItemLink) {
			doCopy(ureq, metadatasCtrl.getItem());
		} else if(source == nextItemLink) {
			fireEvent(ureq, new QItemEvent("next", metadatasCtrl.getItem()));
		} else if(source == previousItemLink) {
			fireEvent(ureq, new QItemEvent("previous", metadatasCtrl.getItem()));
		} else if(source == showMetadataLink) {
			doShowMetadata();
		} else if(source == hideMetadataLink) {
			doHideMetadata();
		} else if(source == stackPanel) {
			if(event instanceof PopEvent) {
				PopEvent pop = (PopEvent)event;
				if(pop.getController() == editMainCtrl) {
					doContentChanged(ureq);
				}
			}
		}
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == selectGroupCtrl) {
			if(event instanceof BusinessGroupSelectionEvent) {
				BusinessGroupSelectionEvent bge = (BusinessGroupSelectionEvent)event;
				List<BusinessGroup> groups = bge.getGroups();
				if(groups.size() > 0) {
					QuestionItem item = (QuestionItem)((SelectBusinessGroupController)source).getUserObject();
					doShareItems(ureq, item, groups);
					metadatasCtrl.updateShares();
				}
			}
			cmc.deactivate();
			cleanUp();
		} else if (source == reviewStartCtrl) {
			if (event == Event.DONE_EVENT) {
				TaxonomyLevel taxonomyLevel = reviewStartCtrl.getSelectedTaxonomyLevel();
				doStartReview(ureq, taxonomyLevel);
			}
			cmc.deactivate();
			cleanUp();	
		} else if (source == reviewCtrl) {
			if (event == Event.DONE_EVENT) {
				float rating = reviewCtrl.getCurrentRatting();
				String comment = reviewCtrl.getComment();
				doRate(ureq, rating, comment);
			}
			cmc.deactivate();
			cleanUp();
		} else if(source == confirmEndOfLifeCtrl) {
			boolean endOfLife = DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event);
			if(endOfLife) {
				QuestionItem item = (QuestionItem)confirmEndOfLifeCtrl.getUserObject();
				doEndOfLife(ureq, item);
			}
		} else if(source == confirmDeleteBox) {
			boolean delete = DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event);
			if(delete) {
				QuestionItem item = (QuestionItem)confirmDeleteBox.getUserObject();
				doDelete(ureq, item);
			}
		} else if(source == cmc) {
			cleanUp();
		} else if(source == editCtrl) {
			if(event == Event.CHANGED_EVENT) {
				doContentChanged(ureq);
			} else if(event instanceof QItemEdited) {
				fireEvent(ureq, event);
			}
		} else if(source == metadatasCtrl) {
			if(event instanceof QItemEdited) {
				fireEvent(ureq, event);
			}
		}
		super.event(ureq, source, event);
	}

	private void cleanUp() {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(selectGroupCtrl);
		removeAsListenerAndDispose(reviewCtrl);
		removeAsListenerAndDispose(reviewStartCtrl);
		cmc = null;
		selectGroupCtrl = null;
		reviewCtrl = null;
		reviewStartCtrl = null;
	}
	
	private void doConfirmStartReview(UserRequest ureq, QuestionItem item) {
		reviewStartCtrl = new ReviewStartController(ureq, getWindowControl(), item);
		listenTo(reviewStartCtrl);
		cmc = new CloseableModalController(getWindowControl(), null,
				reviewStartCtrl.getInitialComponent(), true,
				translate("process.start.review.title"), false);
		listenTo(cmc);
		cmc.activate();
	}

	private void doStartReview(UserRequest ureq, TaxonomyLevel taxonomyLevel) {
		QuestionItem item = metadatasCtrl.getItem();
		if (item instanceof QuestionItemImpl) {
			QuestionItemImpl itemImpl = (QuestionItemImpl) item;
			if (itemImpl.getTaxonomyLevel() == null || !itemImpl.getTaxonomyLevel().equals(taxonomyLevel)) {
				itemImpl.setTaxonomyLevel(taxonomyLevel);
			}
		}
		doChangeQuestionStatus(ureq, item, QuestionStatus.review, "process.review.started");
	}
	
	private void openReview(UserRequest ureq) {
		reviewCtrl = new ReviewController(ureq, getWindowControl());
		listenTo(reviewCtrl);
		cmc = new CloseableModalController(getWindowControl(), null,
				reviewCtrl.getInitialComponent(), true,
				translate("process.rating.title"), false);
		listenTo(cmc);
		cmc.activate();
	}

	private void doRate(UserRequest ureq, float rating, String comment) {
		QuestionItem item = metadatasCtrl.getItem();
		qpoolService.rateItemInReview(item, getIdentity(), rating, comment);
		fireEvent(ureq, new QPoolEvent(QPoolEvent.ITEM_STATUS_CHANGED, item.getKey()));
		reloadData();
	}

	private void doStatusDraft(UserRequest ureq, QuestionItem item) {
		doChangeQuestionStatus(ureq, item, QuestionStatus.draft, "process.draft.set");
	}
	
	private void doStatusRevised(UserRequest ureq, QuestionItem item) {
		doChangeQuestionStatus(ureq, item, QuestionStatus.revised, "process.revision.set");
	}
	
	private void doStatusReview(UserRequest ureq, QuestionItem item) {
		doChangeQuestionStatus(ureq, item, QuestionStatus.review, "process.review.set");
	}
	
	private void doStatusFinal(UserRequest ureq, QuestionItem item) {
		doChangeQuestionStatus(ureq, item, QuestionStatus.finalVersion, "process.final.set");
	}
	
	private void doConfirmEndOfLife(UserRequest ureq, QuestionItem item) {
		String msg = translate("process.confirm.endOfLife", StringHelper.escapeHtml(item.getTitle()));
		confirmEndOfLifeCtrl = activateYesNoDialog(ureq, null, msg, confirmEndOfLifeCtrl);
		confirmEndOfLifeCtrl.setUserObject(item);
	}

	private void doEndOfLife(UserRequest ureq, QuestionItem item) {
		doChangeQuestionStatus(ureq, item, QuestionStatus.endOfLife, "process.endOfLife.set");
	}
	
	private void doChangeQuestionStatus(UserRequest ureq, QuestionItem item, QuestionStatus newStatus, String i18nInfo) {
		if(!newStatus.equals(item.getQuestionStatus()) && item instanceof QuestionItemImpl) {
			QuestionItemImpl itemImpl = (QuestionItemImpl)item;
			itemImpl.setQuestionStatus(newStatus);
			qpoolService.updateItem(itemImpl);
			fireEvent(ureq, new QPoolEvent(QPoolEvent.ITEM_STATUS_CHANGED, item.getKey()));
			showInfo(i18nInfo);
		}
		reloadData();
	}

	private void reloadData() {
		Long itemKey = metadatasCtrl.getItem().getKey();
		QuestionItemView itemView = itemSource.getItemWithoutRestrictions(itemKey);
		if (itemView != null) {
			securityCallback.setQuestionItemView(itemView);
			initTools();
			// TODO uh A muss Metadata ctrl erneuert werden?
//			metadatasCtrl.reloadData(item);
		}
	}

	private void doCopy(UserRequest ureq, QuestionItemShort item) {
		List<QuestionItem> copies = qpoolService.copyItems(getIdentity(), Collections.singletonList(item));
		if(copies.size() == 1) {
			showInfo("item.copied", Integer.toString(copies.size()));
			fireEvent(ureq, new QItemEvent("copy-item", copies.get(0)));
		}
	}
	
	private void doEdit(UserRequest ureq, QuestionItem item) {
		removeAsListenerAndDispose(editCtrl);
		
		QPoolSPI spi = poolModule.getQuestionPoolProvider(item.getFormat());
		editCtrl = spi.getEditableController(ureq, getWindowControl(), item);
		listenTo(editCtrl);
		
		editMainCtrl = new LayoutMain3ColsController(ureq, getWindowControl(), editCtrl);
		stackPanel.pushController("Edition", editMainCtrl);
	}
	
	// TODO uh delete???
	private void doContentChanged(UserRequest ureq) {
		QuestionItem item = metadatasCtrl.updateVersionNumber();
		//update preview
		setPreviewController(ureq, item);
	}
	
	private void doSelectGroup(UserRequest ureq, QuestionItem item) {
		removeAsListenerAndDispose(selectGroupCtrl);
		selectGroupCtrl = new SelectBusinessGroupController(ureq, getWindowControl());
		selectGroupCtrl.setUserObject(item);
		listenTo(selectGroupCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), translate("close"),
				selectGroupCtrl.getInitialComponent(), true, translate("select.group"));
		cmc.activate();
		listenTo(cmc);
	}
	
	private void doShareItems(UserRequest ureq, QuestionItemShort item, List<BusinessGroup> groups) {
		qpoolService.shareItemsWithGroups(Collections.singletonList(item), groups, false);
		fireEvent(ureq, new QPoolEvent(QPoolEvent.ITEM_SHARED));
	}

	private void doConfirmDelete(UserRequest ureq, QuestionItem item) {
		String msg = translate("confirm.delete", StringHelper.escapeHtml(item.getTitle()));
		confirmDeleteBox = activateYesNoDialog(ureq, null, msg, confirmDeleteBox);
		confirmDeleteBox.setUserObject(item);
	}
	
	private void doDelete(UserRequest ureq, QuestionItemShort item) {
		qpoolService.deleteItems(Collections.singletonList(item));
		fireEvent(ureq, new QPoolEvent(QPoolEvent.ITEM_DELETED));
		showInfo("item.deleted");
	}
	
	private void doExport(UserRequest ureq, QuestionItemShort item) {
		ExportQItemResource mr = new ExportQItemResource("UTF-8", getLocale(), item);
		ureq.getDispatchResult().setResultingMediaResource(mr);
	}
	
	private void doShowMetadata() {
		stackPanel.addTool(hideMetadataLink, Align.right);
		stackPanel.removeTool(showMetadataLink);
		mainVC.contextPut("metadataSwitch", Boolean.TRUE);
	}
	
	private void doHideMetadata() {
		stackPanel.addTool(showMetadataLink, Align.right);
		stackPanel.removeTool(hideMetadataLink);
		mainVC.contextPut("metadataSwitch", Boolean.FALSE);
	}

}
