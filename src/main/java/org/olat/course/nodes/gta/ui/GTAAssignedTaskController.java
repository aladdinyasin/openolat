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
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.GTACourseNode;
import org.olat.course.nodes.gta.GTAManager;
import org.olat.course.nodes.gta.Task;
import org.olat.course.nodes.gta.TaskHelper;
import org.olat.course.nodes.gta.model.TaskDefinition;
import org.olat.course.run.environment.CourseEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 05.03.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class GTAAssignedTaskController extends BasicController {
	
	
	
	private final Link downloadButton;
	
	private final File taskFile;
	
	@Autowired
	private GTAManager gtaManager;
	
	public GTAAssignedTaskController(UserRequest ureq, WindowControl wControl, Task task,
			TaskDefinition taskDef, CourseEnvironment courseEnv, GTACourseNode gtaNode,
			String i18nDescription, String message) {
		super(ureq, wControl);

		File taskDir = gtaManager.getTasksDirectory(courseEnv, gtaNode);
		taskFile = new File(taskDir, task.getTaskName());
		
		VelocityContainer mainVC = createVelocityContainer("assigned_task");
		mainVC.contextPut("description", translate(i18nDescription));
		if(StringHelper.containsNonWhitespace(message)) {
			mainVC.contextPut("message", message);
		}
		
		double fileSizeInMB = taskFile.length() / (1024.0d * 1024.0d);
		String[] infos = new String[] { taskFile.getName(), TaskHelper.format(fileSizeInMB) };
		String taskInfos = translate("download.task.infos", infos);
		String cssIcon = CSSHelper.createFiletypeIconCssClassFor(taskFile.getName());
		mainVC.contextPut("taskInfo", taskInfos);
		mainVC.contextPut("taskCssIcon", cssIcon);
		if(taskDef != null) {
			mainVC.contextPut("taskName", taskDef.getTitle());
			mainVC.contextPut("taskDescription", taskDef.getDescription());
		}
		downloadButton = LinkFactory.createButton("download.task", mainVC, this);
		downloadButton.setIconLeftCSS("o_icon o_icon_download");
		downloadButton.setTarget("_blank");
		putInitialPanel(mainVC);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(downloadButton == source) {
			MediaResource mdr = new FileMediaResource(taskFile);
			ureq.getDispatchResult().setResultingMediaResource(mdr);
		}
	}
}
