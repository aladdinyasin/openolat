/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.core.gui.control.generic.dtabs;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;

/**
 * Description:<br>
 * Initial Date:  19.07.2005 <br>
 *
 * @author Felix Jost
 */
public interface DTabs {

	/**
	 * @param ores
	 * @return the tab
	 */
	public DTab getDTab(OLATResourceable ores);

	/**
	 * @param ores
	 * @param repoOres
	 * @param title
	 * @return the tab or null if the headerbar is full. if null, the implementation of the DTabs should issue a warning to the current windowcontrol
	 */
	public DTab createDTab(OLATResourceable ores, OLATResourceable repoOres, String title);

	/**
	 * @param ureq
	 * @param dTab
	 * @param viewIdentifier if null, no activation takes places
	 */
	public void activate(UserRequest ureq, DTab dTab, List<ContextEntry> ce);

	/**
	 * @param ureq
	 * @param className the name of the class implementing the siteinstance we would like to activate
	 * @param viewIdentifier the subcommand (see docu of each controller implementing Activatable
	 */
	public void activateStatic(UserRequest ureq, String className, List<ContextEntry> entries);
	
	/**
	 * adds the tab. (upon Event.DONE of the contained controller && if controller is DTabAware -> controller.dispose called by dtabs)
	 * @param dt
	 */
	public boolean addDTab(UserRequest ureq, DTab dt);

	/**
	 * Remove a tab from tabs-list.
	 * @param dt Remove this tab
	 */
	public void removeDTab(UserRequest ureq, DTab dt);
		
	
}

