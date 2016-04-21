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
package org.olat.modules.video.ui;

import org.olat.core.commons.services.image.Size;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.modules.video.VideoManager;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * First page of video preferences
 * 
 * @author Dirk Furrer, dirk.furrer@frentix.com, http://www.frentix.com
 *
 */
public class VideoMetaDataEditFormController extends FormBasicController {

	protected FormUIFactory uifactory = FormUIFactory.getInstance();
	@Autowired
	private VideoManager videoManager;
	private OLATResource videoResource;
	private RichTextElement descriptionField;

	public VideoMetaDataEditFormController(UserRequest ureq, WindowControl wControl, OLATResource re) {
		super(ureq, wControl);
		videoResource = re;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener,
			UserRequest ureq) {
		setFormTitle("tab.video.metaDataConfig");

		Size videoSize = videoManager.getVideoSize(videoResource);

		uifactory.addStaticTextElement("video.config.width", String.valueOf(videoSize.getWidth()) + "px", formLayout);
		uifactory.addStaticTextElement("video.config.height", String.valueOf(videoSize.getHeight()) + "px", formLayout);
		String aspcectRatio = videoManager.getAspectRatio(videoSize);
		uifactory.addStaticTextElement("video.config.ratio", aspcectRatio, formLayout);
		uifactory.addStaticTextElement("video.config.creationDate", StringHelper.formatLocaleDateTime(videoResource.getCreationDate().getTime(), getLocale()), formLayout);
		uifactory.addStaticTextElement("video.config.fileSize", Formatter.formatBytes(videoManager.getVideoFile(videoResource).length()), formLayout);

		descriptionField = uifactory.addRichTextElementForStringDataMinimalistic("description", "video.config.description", videoManager.getDescription(videoResource), -1, -1, formLayout, getWindowControl());
		uifactory.addFormSubmitButton("submit", formLayout);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		videoManager.setDescription(videoResource, descriptionField.getValue());
	}

	@Override
	protected void doDispose() {

	}

}
