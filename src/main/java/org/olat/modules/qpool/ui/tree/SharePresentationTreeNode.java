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
package org.olat.modules.qpool.ui.tree;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.util.resource.OresHelper;
import org.olat.modules.qpool.ui.SharePresentationController;

/**
 * 
 * Initial date: 19.10.2017<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class SharePresentationTreeNode  extends GenericTreeNode implements ControllerTreeNode {

	private static final long serialVersionUID = -3800071301195017030L;

	public static final OLATResourceable ORES = OresHelper.createOLATResourceableType("SharePresentation");
	private static final String ICON_CSS_CLASS = "o_sel_qpool_shares";
	
	private Controller sharePresentationCtrl;
	
	public SharePresentationTreeNode(String title) {
		super();
		this.setTitle(title);
		this.setIconCssClass(ICON_CSS_CLASS);
	}

	@Override
	public Controller getController(UserRequest ureq, WindowControl wControl) {
		if(sharePresentationCtrl == null) {
			WindowControl swControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ureq, ORES, null,
					wControl, true);
			sharePresentationCtrl = new SharePresentationController(ureq, swControl);
		} 
		return sharePresentationCtrl;
	}

}
