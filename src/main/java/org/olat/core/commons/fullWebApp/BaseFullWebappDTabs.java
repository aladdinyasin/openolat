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
package org.olat.core.commons.fullWebApp;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;

/**
 * 
 * Initial date: 17.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
class BaseFullWebappDTabs implements DTabs {
	
	private final BaseFullWebappController webappCtrl;
	
	public BaseFullWebappDTabs(BaseFullWebappController webappCtrl) {
		this.webappCtrl = webappCtrl;
	}
	
	@Override
	public void activate(UserRequest ureq, DTab dTab, List<ContextEntry> entries) {
		webappCtrl.activate(ureq, dTab, null, entries);
	}

	@Override
	public void activateStatic(UserRequest ureq, String className, List<ContextEntry> entries) {
		webappCtrl.activateStatic(ureq, className, entries);
	}

	@Override
	public boolean addDTab(UserRequest ureq, DTab dt) {
		return webappCtrl.addDTab(ureq, dt);
	}

	@Override
	public DTab createDTab(OLATResourceable ores, OLATResourceable initialOres, String title) {
		return webappCtrl.createDTab(ores, initialOres, title);
	}

	@Override
	public DTab getDTab(OLATResourceable ores) {
		return webappCtrl.getDTab(ores);
	}

	@Override
	public void removeDTab(UserRequest ureq, DTab dt) {
		webappCtrl.removeDTab(ureq, dt);
	}
}