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
package org.olat.course.certificate.ui;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.certificate.Certificate;
import org.olat.course.certificate.CertificateTemplate;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.model.CertificateInfos;
import org.olat.course.config.CourseConfig;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 21.10.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessedIdentityCertificatesController extends BasicController {
	
	private Link generateLink;
	private final VelocityContainer mainVC;
	private DialogBoxController confirmCertificateCtrl;
	
	private final OLATResource resource;
	private final UserCourseEnvironment assessedUserCourseEnv;
	
	@Autowired
	private CertificatesManager certificatesManager;
	
	public AssessedIdentityCertificatesController(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment assessedUserCourseEnv) {
		super(ureq, wControl);

		this.assessedUserCourseEnv = assessedUserCourseEnv;
		resource = assessedUserCourseEnv.getCourseEnvironment().getCourseGroupManager().getCourseResource();
		
		mainVC = createVelocityContainer("certificate_overview");
		loadList();
		
		CourseConfig courseConfig = assessedUserCourseEnv.getCourseEnvironment().getCourseConfig();
		if(courseConfig.isManualCertificationEnabled()) {
			generateLink = LinkFactory.createLink("generate.certificate", "generate", getTranslator(), mainVC, this, Link.BUTTON);
		}
		putInitialPanel(mainVC);
	}
	
	private void loadList() {
		Identity assessedIdentity = assessedUserCourseEnv.getIdentityEnvironment().getIdentity();
		List<Certificate> certificates = certificatesManager.getCertificates(assessedIdentity, resource);
		List<Link> certificatesLink = new ArrayList<>(certificates.size());
		int count = 0;
		for(Certificate certificate:certificates) {
			Link link = LinkFactory.createLink("download." + count++, "download",
					getTranslator(), mainVC, this, Link.NONTRANSLATED);
			link.setCustomDisplayText(certificate.getName());
			link.setIconLeftCSS("o_icon o_icon-lg o_filetype_pdf");
			link.setUserObject(certificate);
			link.setTarget("_blank");
			certificatesLink.add(link);
		}
		mainVC.contextPut("certificates", certificatesLink);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(generateLink == source) {
			doConfirmGenerateCertificate(ureq) ;
		} else if(source instanceof Link) {
			Link link = (Link)source;
			String cmd = link.getCommand();
			if("download".equals(cmd)) {
				Certificate certificate = (Certificate)link.getUserObject();
				doDownload(ureq, certificate);
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(confirmCertificateCtrl == source) {
			if(DialogBoxUIFactory.isYesEvent(event)) {
				doGenerateCertificate();
			}
		}
		super.event(ureq, source, event);
	}

	private void doDownload(UserRequest ureq, Certificate certificate) {
		VFSLeaf certificateLeaf = certificatesManager.getCertificateLeaf(certificate);
		MediaResource resource = new VFSMediaResource(certificateLeaf);
		ureq.getDispatchResult().setResultingMediaResource(resource);
	}
	
	private void doConfirmGenerateCertificate(UserRequest ureq) {
		ICourse course = CourseFactory.loadCourse(resource);
		Identity assessedIdentity = assessedUserCourseEnv.getIdentityEnvironment().getIdentity();
		RepositoryEntry courseEntry = course.getCourseEnvironment().getCourseGroupManager().getCourseEntry();
		if(certificatesManager.isRecertificationAllowed(assessedIdentity, courseEntry)) {
			//don't need to confirm
			doGenerateCertificate();
		} else {
			String title = translate("confirm.certificate.title");
			String text = translate("confirm.certificate.text");
			confirmCertificateCtrl = activateYesNoDialog(ureq, title, text, confirmCertificateCtrl);
		}
	}
	
	private void doGenerateCertificate() {
		ICourse course = CourseFactory.loadCourse(resource);
		CourseNode rootNode = course.getRunStructure().getRootNode();
		Identity assessedIdentity = assessedUserCourseEnv.getIdentityEnvironment().getIdentity();
		ScoreEvaluation scoreEval = assessedUserCourseEnv.getScoreAccounting().getScoreEvaluation(rootNode);
		RepositoryEntry courseEntry = course.getCourseEnvironment().getCourseGroupManager().getCourseEntry();

		CertificateTemplate template = null;
		Long templateKey = course.getCourseConfig().getCertificateTemplate();
		if(templateKey != null) {
			template = certificatesManager.getTemplateById(templateKey);
		}

		Float score = scoreEval == null ? null : scoreEval.getScore();
		Boolean passed = scoreEval == null ? null : scoreEval.getPassed();
		CertificateInfos certificateInfos = new CertificateInfos(assessedIdentity, score, passed);
		MailerResult result = new MailerResult();
		certificatesManager.generateCertificate(certificateInfos, courseEntry, template, result);
		loadList();
	}
}