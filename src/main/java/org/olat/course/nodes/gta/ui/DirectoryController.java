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
package org.olat.course.nodes.gta.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.media.FileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.io.SystemFileFilter;
import org.olat.fileresource.ZippedDirectoryMediaResource;

/**
 * 
 * Initial date: 06.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DirectoryController extends BasicController {
	
	private Link bulkReviewLink;
	
	private final String zipName;
	private final File documentsDir;
	
	public DirectoryController(UserRequest ureq, WindowControl wControl, File documentsDir,
			String i18nDescription) {
		this(ureq, wControl, documentsDir, i18nDescription, null, null);
	}
	
	public DirectoryController(UserRequest ureq, WindowControl wControl, File documentsDir,
			String i18nDescription, String i18nBulkDownload, String zipName) {
		super(ureq, wControl);
		this.zipName = zipName;
		this.documentsDir = documentsDir;

		VelocityContainer mainVC = createVelocityContainer("documents_readonly");
		mainVC.contextPut("description", translate(i18nDescription));
		mainVC.contextPut("zipName", zipName + ".zip");
		
		if(StringHelper.containsNonWhitespace(i18nBulkDownload)) {
			bulkReviewLink = LinkFactory.createCustomLink("bulk", "bulk", i18nBulkDownload, Link.BUTTON, mainVC, this);
			bulkReviewLink.setIconLeftCSS("o_icon o_icon_download");
		}
		
		List<String> linkNames = new ArrayList<>();
		File[] documents = documentsDir.listFiles(SystemFileFilter.FILES_ONLY);
		for(File document:documents) {
			Link link = LinkFactory.createLink("doc-" + CodeHelper.getRAMUniqueID(), "download", getTranslator(), mainVC, this, Link.NONTRANSLATED);
			link.setCustomDisplayText(StringHelper.escapeHtml(document.getName()));
			String cssClass = CSSHelper.createFiletypeIconCssClassFor(document.getName());
			link.setIconLeftCSS("o_icon o_icon-fw " + cssClass);
			link.setUserObject(document);
			link.setTarget("_blank");
			linkNames.add(link.getComponentName());
		}
		mainVC.contextPut("linkNames", linkNames);

		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(bulkReviewLink == source) {
			doBulkdownload(ureq);
		} else if(source instanceof Link && "download".equals(((Link)source).getCommand())) {
			Link downloadLink = (Link)source;
			doDownload(ureq, (File)downloadLink.getUserObject());
		}
	}
	
	private void doDownload(UserRequest ureq, File file) {
		MediaResource mdr = new FileMediaResource(file);
		ureq.getDispatchResult().setResultingMediaResource(mdr);
	}
	
	private void doBulkdownload(UserRequest ureq) {
		MediaResource mdr = new ZippedDirectoryMediaResource(zipName, documentsDir);
		ureq.getDispatchResult().setResultingMediaResource(mdr);
	}
}