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
package org.olat.course.nodes.members;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.modules.bc.FileUploadController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.Identity;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailContext;
import org.olat.core.util.mail.MailContextImpl;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailLoggingAction;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailModule;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.ui.EMailIdentity;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 21.12.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MembersMailController extends FormBasicController {
	
	private static final String[] keys = new String[]{ "on" };
	
	private RichTextElement bodyEl;
	private FileElement attachmentEl;
	private FormLink addMemberButton;
	private TextElement subjectEl, externalAddressesEl;
	private MultipleSelectionElement ownerEl, coachEl, participantEl, individualEl, externalEl, copyFromEl;
	private FormLayoutContainer uploadCont, individualMemberCont;
	
	private CloseableModalController cmc;
	private SelectMembersController selectMemberCtrl;
	
	private int counter = 0;
	private long attachmentSize = 0l;
	private File attachementTempDir;
	private final CourseEnvironment courseEnv;
	private final int contactAttachmentMaxSizeInMb;
	private final List<Member> selectedMembers = new ArrayList<>();
	private final List<Attachment> attachments = new ArrayList<>();
	private final List<Member> ownerList, coachList, participantList;
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private MailManager mailService;
	@Autowired
	private MailModule mailModule;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private RepositoryService repositoryService;
	
	public MembersMailController(UserRequest ureq, WindowControl wControl, CourseEnvironment courseEnv,
			List<Member> ownerList, List<Member> coachList, List<Member> participantList) {
		super(ureq, wControl, Util.createPackageTranslator(MailHelper.class, ureq.getLocale()));
		
		this.courseEnv = courseEnv;
		this.ownerList = ownerList;
		this.coachList = coachList;
		this.participantList = participantList;
		this.contactAttachmentMaxSizeInMb = mailModule.getMaxSizeForAttachement();
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		String fullName = userManager.getUserDisplayName(getIdentity());
		if(StringHelper.containsNonWhitespace(fullName)) {
			fullName = "[" + fullName + "]";
		}
		TextElement fromEl = uifactory.addTextElement("from", "email.from", 255, fullName, formLayout);
		fromEl.setEnabled(false);
		
		uifactory.addSpacerElement("space-1", formLayout, false);
		
		String to = "send.mail.to";

		if(ownerList != null && ownerList.size() > 0) {
			String[] values = new String[] { translate("contact.all.owners") };
			ownerEl = uifactory.addCheckboxesHorizontal("contact.all.owners", to, formLayout, keys, values);
			ownerEl.select(keys[0], true);
			to = null;
		}
		if(coachList != null && coachList.size() > 0) {
			String[] values = new String[] { translate("contact.all.coaches") };
			coachEl = uifactory.addCheckboxesHorizontal("contact.all.coaches", to, formLayout, keys, values);
			coachEl.select(keys[0], true);
			to = null;
		}
		if(participantList != null && participantList.size() > 0) {
			String[] values = new String[] { translate("contact.all.participants") };
			participantEl = uifactory.addCheckboxesHorizontal("contact.all.participants", to, formLayout, keys, values);
			to = null;
		}
		
		if((ownerList != null && ownerList.size() > 0)
				|| (coachList != null && coachList.size() > 0)
				|| (participantList != null && participantList.size() > 0)) {
			String[] values = new String[] { translate("contact.individual") };
			individualEl = uifactory.addCheckboxesHorizontal("contact.individual", to, formLayout, keys, values);
			individualEl.addActionListener(FormEvent.ONCHANGE);
			to = null;

			String attachmentPage = velocity_root + "/individual_members.html";
			individualMemberCont = FormLayoutContainer.createCustomFormLayout("contact.individual.list", getTranslator(), attachmentPage);
			individualMemberCont.setRootForm(mainForm);
			individualMemberCont.setVisible(false);
			individualMemberCont.contextPut("selectedMembers", selectedMembers);
			formLayout.add(individualMemberCont);
			
			addMemberButton = uifactory.addFormLink("add.member", "add", "", "", individualMemberCont, Link.NONTRANSLATED);
			addMemberButton.setIconLeftCSS("o_icon o_icon-lg o_icon_table_large");
			addMemberButton.setDomReplacementWrapperRequired(false);
			((Link)addMemberButton.getComponent()).setSuppressDirtyFormWarning(true);
		}

		String[] extValues = new String[] { translate("contact.external") };
		externalEl = uifactory.addCheckboxesHorizontal("contact.external", to, formLayout, keys, extValues);
		externalEl.addActionListener(FormEvent.ONCHANGE);
		
		externalAddressesEl = uifactory.addTextAreaElement("contact.external.list", null, 4096, 3, 60, false, "", formLayout);
		externalAddressesEl.setExampleKey("contact.external.list.example", null);
		externalAddressesEl.setVisible(false);

		uifactory.addSpacerElement("space-2", formLayout, false);
		
		subjectEl = uifactory.addTextElement("subject", "mail.subject", 255, "", formLayout);
		subjectEl.setDisplaySize(255);
		subjectEl.setMandatory(true);
		bodyEl = uifactory.addRichTextElementForStringDataMinimalistic("body", "mail.body", "", 15, 8, formLayout, getWindowControl());
		bodyEl.setMandatory(true);
		
		attachmentEl = uifactory.addFileElement(getWindowControl(), "file_upload_1", "contact.attachment", formLayout);
		attachmentEl.addActionListener(FormEvent.ONCHANGE);
		attachmentEl.setExampleKey("contact.attachment.maxsize", new String[]{ Integer.toString(contactAttachmentMaxSizeInMb) });
		
		String attachmentPage = velocity_root + "/attachments.html";
		uploadCont = FormLayoutContainer.createCustomFormLayout("file_upload_inner", getTranslator(), attachmentPage);
		uploadCont.setRootForm(mainForm);
		uploadCont.setVisible(false);
		uploadCont.contextPut("attachments", attachments);
		formLayout.add(uploadCont);
		
		String[] copyValues = new String[] { "" };
		copyFromEl = uifactory.addCheckboxesHorizontal("copy.from", "contact.cp.from", formLayout, keys, copyValues);
		
		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		uifactory.addFormSubmitButton("email.send", buttonGroupLayout);
		uifactory.addFormCancelButton("cancel", buttonGroupLayout, ureq, getWindowControl());
	}
	
	@Override
	protected void doDispose() {
		if(attachementTempDir != null && attachementTempDir.exists()) {
			FileUtils.deleteDirsAndFiles(attachementTempDir, true, true);
		}
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		subjectEl.clearError();
		if(!StringHelper.containsNonWhitespace(subjectEl.getValue())) {
			subjectEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		}
		
		bodyEl.clearError();
		if(!StringHelper.containsNonWhitespace(bodyEl.getValue())) {
			bodyEl.setErrorKey("form.legende.mandatory", null);
			allOk &= false;
		}
		
		externalAddressesEl.clearError();
		if(externalEl != null && externalEl.isAtLeastSelected(1)) {
			String value = externalAddressesEl.getValue();
			StringBuilder errors = new StringBuilder();
			if(StringHelper.containsNonWhitespace(value)) {
				for(StringTokenizer tokenizer= new StringTokenizer(value, ",\r\n", false); tokenizer.hasMoreTokens(); ) {
					String email = tokenizer.nextToken().trim();
					if(!MailHelper.isValidEmailAddress(email)) {
						if(errors.length() > 0) errors.append(", ");
						errors.append(email);
					}
				}
			}
			
			if(errors.length() > 0) {
				externalAddressesEl.setErrorKey("mailhelper.error.addressinvalid", new String[]{ errors.toString() });
				allOk &= false;
			}
		}
		
		return allOk & super.validateFormLogic(ureq);
	}
	
	private File[] getAttachments() {
		File[] atttachmentArr = new File[attachments.size()];
		for(int i=attachments.size(); i-->0; ) {
			atttachmentArr[i] = attachments.get(i).getFile();
		}
		return atttachmentArr;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == externalEl) {
			externalAddressesEl.setVisible(externalEl.isAtLeastSelected(1));
		} else if(source == individualEl) {
			individualMemberCont.setVisible(individualEl.isAtLeastSelected(1));
			flc.setDirty(true);
		} else if(source == attachmentEl) {
			doUploadAttachement();
		} else if(source == addMemberButton) {
			doChooseMember(ureq);
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("delete".equals(cmd)) {
				Attachment attachment = (Attachment)link.getUserObject();
				doDeleteAttachment(attachment);
			} else if("remove".equals(cmd)) {
				Member member = (Member)link.getUserObject();
				doRemoveIndividualMember(member);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == selectMemberCtrl) {
			if(event == Event.DONE_EVENT) {
				List<Member> moreSelectedMembers = selectMemberCtrl.getSelectedMembers();
				doAddSelectedMembers(moreSelectedMembers);
			}
			cmc.deactivate();
			cleanUp();
		} else if(source == cmc) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(selectMemberCtrl);
		removeAsListenerAndDispose(cmc);
		selectMemberCtrl = null;
		cmc = null;
	}
	
	private void doAddSelectedMembers(List<Member> moreSelectedMembers) {
		if(moreSelectedMembers == null || moreSelectedMembers.isEmpty()) return;
		
		for(Member member:moreSelectedMembers) {
			if(selectedMembers.contains(member)) continue;
			
			if(member.getRemoveLink() == null) {
				FormLink removeLink = uifactory.addFormLink("remove_" + (++counter), "remove", "", null, individualMemberCont, Link.NONTRANSLATED);
				removeLink.setUserObject(member);
				removeLink.setIconLeftCSS("o_icon o_icon_remove");
				individualMemberCont.add(removeLink);
				individualMemberCont.add("remove_" + (++counter), removeLink);
				member.setRemoveLink(removeLink);
			} else {
				individualMemberCont.add(member.getRemoveLink());
				individualMemberCont.add(member.getRemoveComponentName(), member.getRemoveLink());	
			}
			selectedMembers.add(member);
		}
	}
	
	private void doRemoveIndividualMember(Member member) {
		selectedMembers.remove(member);
		individualMemberCont.setDirty(true);
	}

	private void doChooseMember(UserRequest ureq) {
		if(selectMemberCtrl != null || cmc != null) return;
		
		List<Member> owners = ownerList;
		List<Member> coaches = coachList;
		List<Member> participants = participantList;
		if(ownerEl != null && ownerEl.isAtLeastSelected(1)) {
			owners = null;
		}
		if(coachEl != null && coachEl.isAtLeastSelected(1)) {
			coaches = null;
		}
		if(participantEl != null && participantEl.isAtLeastSelected(1)) {
			participants = null;
		}
		
		if(owners == null || coaches == null && participants == null) {
			showWarning("already.all.selected");
		} else {
			selectMemberCtrl = new SelectMembersController(ureq, getWindowControl(), selectedMembers, owners, coaches, participants);
			listenTo(selectMemberCtrl);
			
			String title = translate("select.members");
			cmc = new CloseableModalController(getWindowControl(), translate("close"), selectMemberCtrl.getInitialComponent(), true, title);
			cmc.suppressDirtyFormWarningOnClose();
			listenTo(cmc);
			cmc.activate();
		}
	}
	
	private void doDeleteAttachment(Attachment attachment) {
		attachmentSize -= attachment.getFile().length();
		attachment.getFile().delete();
		attachments.remove(attachment);
		uploadCont.setVisible(attachments.size() > 0);
		uploadCont.setDirty(true);
	}
	
	private void doUploadAttachement() {
		if(attachementTempDir == null) {
			attachementTempDir = FileUtils.createTempDir("attachements", null, null);
		}
		
		long size = attachmentEl.getUploadSize();
		String filename = attachmentEl.getUploadFileName();
		if(size + attachmentSize > (contactAttachmentMaxSizeInMb  * 1024 * 1024)) {
			showWarning("contact.attachment,maxsize", Integer.toString(contactAttachmentMaxSizeInMb));
			attachmentEl.reset();
		} else {
			File attachment = attachmentEl.moveUploadFileTo(attachementTempDir);
			attachmentEl.reset();
			if(attachment == null) {
				logError("Could not move contact-form attachment to " + attachementTempDir.getAbsolutePath(), null);
				setTranslator(Util.createPackageTranslator(FileUploadController.class, getLocale(), getTranslator()));
				showError("FileMoveCopyFailed","");
			} else {
				attachmentSize += size;
				FormLink removeFile = uifactory.addFormLink("delete_" + (++counter), "delete", "", null, uploadCont, Link.LINK);
				removeFile.setIconLeftCSS("o_icon o_icon-fw o_icon_delete");
				String css = CSSHelper.createFiletypeIconCssClassFor(filename);
				Attachment wrapper = new Attachment(attachment, attachment.getName(), css, removeFile);
				removeFile.setUserObject(wrapper);
				attachments.add(wrapper);
				uploadCont.setVisible(true);
			}
		}
	}
	
	private void doSend(UserRequest ureq) {
		ContactList contactList = new ContactList("");
		if(ownerEl != null && ownerEl.isAtLeastSelected(1)) {
			RepositoryEntry courseRepositoryEntry = courseEnv.getCourseGroupManager().getCourseEntry();
			List<Identity> owners = repositoryService.getMembers(courseRepositoryEntry, GroupRoles.owner.name());
			contactList.addAllIdentites(owners);
		}
		
		if(coachEl != null && coachEl.isAtLeastSelected(1)) {
			Set<Long> sendToWhatYouSee = new HashSet<>();
			for(Member coach:coachList) {
				sendToWhatYouSee.add(coach.getKey());
			}
			CourseGroupManager cgm = courseEnv.getCourseGroupManager();
			avoidInvisibleMember(cgm.getCoachesFromBusinessGroups(), contactList, sendToWhatYouSee);
			avoidInvisibleMember(cgm.getCoaches(), contactList, sendToWhatYouSee);
		}
		
		if(participantEl != null && participantEl.isAtLeastSelected(1)) {
			Set<Long> sendToWhatYouSee = new HashSet<>();
			for(Member participant:participantList) {
				sendToWhatYouSee.add(participant.getKey());
			}
			CourseGroupManager cgm = courseEnv.getCourseGroupManager();
			avoidInvisibleMember(cgm.getParticipantsFromBusinessGroups(), contactList, sendToWhatYouSee);
			avoidInvisibleMember(cgm.getParticipants(), contactList, sendToWhatYouSee);
		}
		
		if(individualEl != null && individualEl.isAtLeastSelected(1)
				&& selectedMembers != null && selectedMembers.size() > 0) {
			List<Long> identityKeys = new ArrayList<>(selectedMembers.size());
			for(Member member:selectedMembers) {
				identityKeys.add(member.getKey());
			}
			List<Identity> selectedIdentities = securityManager.loadIdentityByKeys(identityKeys);
			contactList.addAllIdentites(selectedIdentities);
		}
		
		if(externalEl != null && externalEl.isAtLeastSelected(1)) {
			String value = externalAddressesEl.getValue();
			if(StringHelper.containsNonWhitespace(value)) {
				for(StringTokenizer tokenizer= new StringTokenizer(value, ",\r\n", false); tokenizer.hasMoreTokens(); ) {
					String email = tokenizer.nextToken().trim();
					contactList.add(new EMailIdentity(email, getLocale()));
				}
			}
		}

		doSendEmailToMember(ureq, contactList);
	}
	
	private void doSendEmailToMember(UserRequest ureq, ContactList contactList) {
		boolean success = false;
		try {
			File[] attachmentArr = getAttachments();
			MailContext context = new MailContextImpl(getWindowControl().getBusinessControl().getAsString());
			MailBundle bundle = new MailBundle();
			bundle.setContext(context);
			bundle.setFromId(getIdentity());						
			bundle.setContactLists(Collections.singletonList(contactList));
			bundle.setContent(subjectEl.getValue(), bodyEl.getValue(), attachmentArr);
			MailerResult result = mailService.sendMessage(bundle);
			if(copyFromEl.isAtLeastSelected(1)) {
				MailBundle ccBundle = new MailBundle();
				ccBundle.setContext(context);
				ccBundle.setFromId(getIdentity()); 
				ccBundle.setCc(getIdentity());							
				ccBundle.setContent(subjectEl.getValue(), bodyEl.getValue(), attachmentArr);
				MailerResult ccResult = mailService.sendMessage(ccBundle);
				result.append(ccResult);
			}
			success = result.isSuccessful();
		} catch (Exception e) {
			//error in recipient email address(es)
			handleAddressException(success);
		}
		if (success) {
			showInfo("msg.send.ok");
			// do logging
			ThreadLocalUserActivityLogger.log(MailLoggingAction.MAIL_SENT, getClass());
			fireEvent(ureq, Event.DONE_EVENT);
		} else {
			showInfo("error.msg.send.nok");
			fireEvent(ureq, Event.FAILED_EVENT);
		}
	}
	
	private void handleAddressException(boolean success) {
		StringBuilder error = new StringBuilder();
		if (success) {
			error.append(translate("error.msg.send.partially.nok"))
			     .append("<br />")
			     .append(translate("error.msg.send.invalid.rcps"));
		} else {
			error.append(translate("error.msg.send.nok"))
			     .append("<br />")
			     .append(translate("error.msg.send.553"));
		}
		getWindowControl().setError(error.toString());
	}
	
	private void avoidInvisibleMember(List<Identity> members, ContactList contactList, Set<Long> sendToWhatYouSee) {
		for(Identity member:members) {
			if(sendToWhatYouSee.contains(member.getKey())) {
				contactList.add(member);
			}
		}
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSend(ureq);
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	public static class Attachment {
		
		private final File file;
		private final String filename;
		private final String cssClass;
		private final FormLink deleteLink;
		
		public Attachment(File file, String filename, String cssClass, FormLink deleteLink) {
			this.file = file;
			this.filename = filename;
			this.cssClass = cssClass;
			this.deleteLink = deleteLink;
		}
		
		public File getFile() {
			return file;
		}

		public String getCssClass() {
			return cssClass;
		}

		public String getFilename() {
			return filename;
		}

		public FormLink getDeleteLink() {
			return deleteLink;
		}
		
		public String getDeleteComponentName() {
			return deleteLink.getComponent().getComponentName();
		}
	}
}