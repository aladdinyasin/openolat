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
package org.olat.ims.qti21.ui;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.assessment.AssessmentToolOptions;
import org.olat.modules.assessment.ui.AssessableResource;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Initial date: 7 nov. 2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QTI21AssessableResource extends AssessableResource {
	
	public QTI21AssessableResource(boolean hasScore, boolean hasPassed, boolean hasAttempts, boolean hasComments,
			Double minScore, Double maxScore, Double cutValue) {
		super(hasScore, hasPassed, hasAttempts, hasComments, minScore, maxScore, cutValue);
	}

	@Override
	public List<Controller> createAssessmentTools(UserRequest ureq, WindowControl wControl,
			TooledStackedPanel stackPanel, RepositoryEntry entry, AssessmentToolOptions options) {

		Controller resetToolCtrl = new QTI21ResetToolController(ureq, wControl, entry, options);
		List<Controller> toolsCtrl = new ArrayList<>(1);
		toolsCtrl.add(resetToolCtrl);
		return toolsCtrl;
	}
}