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
package org.olat.ims.qti;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSContainerMapper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.ims.qti.editor.ItemNodeTabbedFormController;
import org.olat.ims.qti.editor.QTIEditHelper;
import org.olat.ims.qti.editor.QTIEditorPackage;
import org.olat.ims.qti.editor.beecom.objects.Item;
import org.olat.ims.qti.editor.beecom.objects.QTIDocument;
import org.olat.ims.qti.qpool.QTI12ItemEditorPackage;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItem;
/**
 * 
 * Initial date: 21.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI12EditorController extends BasicController {
	
	private final TabbedPane mainPanel;
	private final VelocityContainer mainVC;
	private ItemNodeTabbedFormController previewCtrl;
	
	private final QPoolService qpoolService;

	public QTI12EditorController(UserRequest ureq, WindowControl wControl, QuestionItem qitem) {
		super(ureq, wControl);
		qpoolService = CoreSpringFactory.getImpl(QPoolService.class);

		mainVC = createVelocityContainer("qti_preview");
		mainPanel = new TabbedPane("tabbedPane", ureq.getLocale());
		
		VFSLeaf leaf = qpoolService.getRootFile(qitem);
		if(leaf == null) {
			//no data to preview
		} else {
			Item item = QTIEditHelper.readItemXml(leaf);
			if(item != null) {
				Translator translator = Util.createPackageTranslator(QTIEditorPackage.class, getLocale());
				VFSContainer directory = qpoolService.getRootDirectory(qitem);
				String mapperUrl = registerMapper(ureq, new VFSContainerMapper(directory));
				QTIDocument doc = new QTIDocument();
				QTIEditorPackage qtiPackage = new QTI12ItemEditorPackage(item, doc, mapperUrl, leaf, directory);
				previewCtrl = new ItemNodeTabbedFormController(item, qtiPackage, ureq, getWindowControl(), translator, false);
				previewCtrl.addTabs(mainPanel);
				listenTo(previewCtrl);
			}
		}
		
		mainVC.put("preview", mainPanel);
		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}
}
